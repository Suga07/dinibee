#!/usr/bin/env python3
"""Source/contract checks only: does not compile dependencies or execute SQL."""
from pathlib import Path
import json,re,hashlib,xml.etree.ElementTree as ET,subprocess,tempfile,collections
root=Path(__file__).resolve().parents[1]
sha=lambda b:hashlib.sha256(b).hexdigest()
refs=json.loads((root/'scripts/qa/phase4_preservation.json').read_text())
for name,want in refs['protected_files'].items():assert sha((root/name).read_bytes())==want,('Changed protected file',name)
for f in root.rglob('*.json'):json.loads(f.read_text())
ET.parse(root/'pom.xml')
collection=json.loads((root/'postman/BookEat_Master.postman_collection.json').read_text())
env=json.loads((root/'postman/BookEat_Local.postman_environment.json').read_text())
def flatten(xs):
 for x in xs:
  if 'request' in x:yield x
  yield from flatten(x.get('item',[]))
requests=list(flatten(collection['item']));hashes=collections.Counter(sha(json.dumps(x,sort_keys=True).encode()) for x in requests)
for h in refs['postman_request_hashes']:assert hashes[h]>0,('Lost/changed original Postman request',h)
variables={x['key'] for x in env['values']}
used=set(re.findall(r'\{\{(\w+)\}\}',json.dumps(collection)));assert used<=variables,('Missing environment variables',used-variables)
scripts=[]
for r in requests:
 for e in r.get('event',[]):scripts.append('{\n'+'\n'.join(e['script']['exec'])+'\n}')
 if r['request'].get('body',{}).get('mode')=='raw':
  raw=r['request']['body']['raw']
  # Existing upload requests may be plain text; JSON bodies must parse after typed substitutions.
  if raw.lstrip().startswith('{'):
   raw=re.sub(r'(?<!")\{\{\w+\}\}(?!")','0',raw);json.loads(raw)
with tempfile.TemporaryDirectory() as tmp:
 f=Path(tmp)/'postman.js';f.write_text('\n'.join(scripts));subprocess.run(['node','--check',str(f)],check=True)
main=root/'src/main/java';sources=list((root/'src').rglob('*.java'))
# Resolve explicit local imports to a top-level source (nested records/static members allowed).
for f in sources:
 for name in re.findall(r'import\s+(?:static\s+)?(com\.bookeat\.[\w.]+);',f.read_text()):
  parts=name.split('.');found=False
  while len(parts)>2:
   if (main/('/'.join(parts)+'.java')).exists() or (root/'src/test/java'/('/'.join(parts)+'.java')).exists():found=True;break
   parts.pop()
  assert found,('Unresolved local import',str(f),name)
# Scalar commerce entity columns and nullability against their CREATE TABLE source.
sql='\n'.join(f.read_text() for f in (root/'src/main/resources/db/migration').glob('*.sql'))
entity_count=column_count=0
for module in ['discovery','reservation','order','diniticket']:
 for f in (main/f'com/bookeat/{module}/entity').glob('*.java'):
  source=f.read_text();table=re.search(r'@Table\(name="([^"]+)"',source)[1]
  block=re.search(r'CREATE TABLE '+table+r'\s*\((.*?)\n\);',sql,re.S|re.I);assert block,('Missing table',table)
  for ann,typ,field in re.findall(r'@Column\(([^)]*)\)\s+private\s+([\w.]+)\s+(\w+);',source):
   named=re.search(r'name="([^"]+)"',ann);column=named[1] if named else re.sub(r'(?<!^)(?=[A-Z])','_',field).lower()
   definition=re.search(r'^\s*'+column+r'\s+([^\n]+)',block[1],re.M);assert definition,('Missing column',table,column)
   if 'nullable=false' in ann:assert 'NOT NULL' in definition[1],('Nullability mismatch',table,column)
   column_count+=1
  entity_count+=1
# Local relative Markdown file targets; ignore web/anchor links.
links=0
for f in [root/'README.md',*list((root/'docs').glob('*.md'))]:
 for target in re.findall(r'\]\(([^)]+)\)',f.read_text()):
  if ':' in target or target.startswith('#'):continue
  target=target.split('#')[0]
  if target:assert (f.parent/target).exists(),('Broken local document link',f,target);links+=1
result={'protected_files_unchanged':len(refs['protected_files']),'original_postman_requests_preserved':len(refs['postman_request_hashes']),'postman_requests':len(requests),'postman_script_blocks_parsed':len(scripts),'environment_variables':len(variables),'java_files_imports_checked':len(sources),'commerce_entities_columns_checked':entity_count,'commerce_columns_checked':column_count,'local_document_links_checked':links,'junit_unit_methods':sum(len(re.findall(r'@Test\b',f.read_text())) for f in (root/'src/test/java').rglob('*Test.java')),'junit_integration_methods':sum(len(re.findall(r'@Test\b',f.read_text())) for f in (root/'src/test/java').rglob('*IT.java'))}
print(json.dumps(result,indent=2))
