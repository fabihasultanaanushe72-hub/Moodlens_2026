#!/usr/bin/env python3
"""Offline structural checks and SQLite integration checks; not an Android build."""
import hashlib,json,re,sqlite3,struct,sys,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'
RES=MAIN/'res'
JAVA=MAIN/'java/com/example/moodlens'
ANDROID='{http://schemas.android.com/apk/res/android}'
checks=[]
def passed(message):
    checks.append(message); print('PASS',message)
# Parse all Android XML and collect named resources.
known=set()
for p in RES.rglob('*'):
    if p.is_file():
        folder=p.parent.name.split('-')[0]
        if folder!='values':known.add((folder,p.stem))
        if p.suffix=='.xml':
            root=ET.parse(p).getroot()
            if folder=='values':
                for child in root:
                    if child.get('name'):known.add((child.tag,child.get('name')))
            for node in root.iter():
                id=node.get(ANDROID+'id','')
                if id.startswith('@+id/'):known.add(('id',id.split('/')[1]))
passed('All resource XML parses')
missing=[]
for p in RES.rglob('*.xml'):
    for category,name in re.findall(r'@(?:\+)?(color|drawable|layout|mipmap|style|string|id)/([\w.]+)',p.read_text()):
        if (category,name) not in known:missing.append((p.name,category,name))
for p in JAVA.glob('*'):
    for category,name in re.findall(r'(?<!android\.)\bR\.(id|drawable|layout|color|string)\.(\w+)',p.read_text()):
        if (category,name) not in known:missing.append((p.name,category,name))
assert not missing, missing
passed('All local XML and source resource references resolve')
# Check that findViewById casts match the actual element type on each Activity screen.
for p in JAVA.glob('*Activity.kt'):
    s=p.read_text();m=re.search(r'setContentView\(R.layout.(\w+)\)',s)
    if not m:continue
    nodes={node.get(ANDROID+'id','').split('/')[-1]:node.tag.split('.')[-1] for node in ET.parse(RES/'layout'/f'{m[1]}.xml').getroot().iter() if node.get(ANDROID+'id')}
    for typ,id in re.findall(r'findViewById<(\w+)>\(R.id.(\w+)\)',s):
        assert id in nodes,(p.name,id)
        actual=nodes[id]
        assert typ=='View' or actual==typ or (typ=='Button' and actual=='AppCompatButton') or (typ=='ImageButton' and actual=='AppCompatImageButton'),(p.name,id,typ,actual)
passed('Explicit Activity view casts match layout elements')
manifest=ET.parse(MAIN/'AndroidManifest.xml').getroot()
for node in manifest.findall('./application/activity'):
    name=node.get(ANDROID+'name').lstrip('.')
    assert (JAVA/(name+'.kt')).is_file(),name
for p in RES.glob('layout/*.xml'):
    for node in ET.parse(p).getroot().iter():
        if node.tag.startswith('com.example.moodlens.'):
            assert (JAVA/(node.tag.split('.')[-1]+'.kt')).is_file()
passed('Manifest activities and custom layout views exist')
# Read the supplied TFLite FlatBuffer without evaluating the PyTorch pickle.
b=(MAIN/'assets/emotion_model.tflite').read_bytes()
u=lambda o:struct.unpack_from('<I',b,o)[0]
h=lambda o:struct.unpack_from('<H',b,o)[0]
def field(t,n):
    v=t-struct.unpack_from('<i',b,t)[0];offset=h(v+4+2*n) if 4+2*n<h(v) else 0
    return t+offset if offset else None
def ref(o):return o+u(o)
def vec(t,n):
    o=field(t,n)
    if o is None:return []
    v=ref(o);return [v+4+4*i for i in range(u(v))]
def ints(t,n):return [struct.unpack_from('<i',b,o)[0] for o in vec(t,n)]
assert b[4:8]==b'TFL3'
m=u(0);assert u(field(m,0))==3
s=ref(vec(m,2)[0]);tensors=[ref(x) for x in vec(s,0)]
inputs=ints(s,1);outputs=ints(s,2)
assert len(inputs)==len(outputs)==1
inp,out=tensors[inputs[0]],tensors[outputs[0]]
assert ints(inp,0)==[1,224,224,3] and ints(out,0)==[1,7]
assert all(field(t,1) is None or b[field(t,1)]==0 for t in [inp,out])
ops=[]
for o in vec(m,1):
    t=ref(o);assert field(t,1) is None,'Custom op needs runtime verification'
    code=u(field(t,3)) if field(t,3) else (b[field(t,0)] if field(t,0) else 0);ops.append(code)
for o in vec(s,3):
    op=ref(o);index=u(field(op,0)) if field(op,0) else 0
    assert index<len(ops)
    assert all(-1<=i<len(tensors) for i in ints(op,1)+ints(op,2))
assert 25 not in ops,'Softmax already inside graph; review app output processing'
labels=(MAIN/'assets/labels.txt').read_text().split()
assert [x.lower() for x in labels]==['angry','disgust','fear','happy','neutral','sad','surprise']
metadata={'file_size_bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'format':'TFLite schema v3','input_shape':ints(inp,0),'output_shape':ints(out,0),'input_type':'FLOAT32','output_type':'FLOAT32','labels':labels,'builtin_operator_codes':ops,'custom_operators':False,'inference_executed':False}
(ROOT/'docs/model_metadata.json').write_text(json.dumps(metadata,indent=2)+'\n')
passed('TFLite input/output, operator references, label order, and no embedded softmax verified')
assert 'noCompress += "tflite"' in (ROOT/'app/build.gradle.kts').read_text()
passed('Build configuration leaves the mapped TFLite asset uncompressed')
# Execute the exact SQL used by the DAO against SQLite fixtures.
db=sqlite3.connect(':memory:')
db.execute('CREATE TABLE detections (id INTEGER PRIMARY KEY, emotion TEXT, confidence REAL, timestamp INTEGER, source TEXT, targetMatched INTEGER)')
for id,source in enumerate(['LIVE','TARGET','CAMERA','CAMERA PHOTO','PHOTO','GALLERY','GALLERY'],1):
    db.execute('INSERT INTO detections VALUES (?,?,?,?,?,?)',(id,'Happy',.6,1000,source,0))
queries=re.findall(r'@Query\("([^"\n]+)"\)',(JAVA/'DetectionDao.java').read_text())
page=next(x for x in queries if 'LIMIT' in x);count=next(x for x in queries if 'COUNT' in x)
for source,expected in [('ALL',7),('LIVE',2),('CAMERA',3),('GALLERY',2)]:
    params={'source':source,'limit':50,'offset':0}
    rows=db.execute(page,params).fetchall()
    assert len(rows)==expected and db.execute(count,params).fetchone()[0]==expected
    assert [r[0] for r in rows]==sorted([r[0] for r in rows],reverse=True)
assert [r[0] for r in db.execute(page,{'source':'ALL','limit':2,'offset':2})]==[5,4]
db.execute(next(x for x in queries if x.startswith('DELETE')))
assert db.execute(count,{'source':'ALL'}).fetchone()[0]==0
passed('DAO SQL: source filters, legacy camera labels, deterministic paging, counts, and deletion')
print('\nSTRUCTURAL CHECKS PASSED. Android build, native inference, and device UI have not been verified.')
