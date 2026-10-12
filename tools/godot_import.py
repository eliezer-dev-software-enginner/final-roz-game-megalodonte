"""Godot text resource reader and asset conversion for the Megalodonte port."""
import base64,collections,functools,hashlib,json,math,re,struct,uuid
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parent.parent
SOURCE=ROOT/'upstream'
ASSETS={};WARNINGS=[]
NUM=r'[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?'
def attrs(header):return dict(re.findall(r'(\w+)="([^"]*)"',header))
@functools.lru_cache(maxsize=8192)
def field(body,key,default=''):return next((line.split(' = ',1)[1] for line in body.splitlines() if line.startswith(key+' = ')),default)
def numbers(value):return [float(v) for v in re.findall(NUM,value)]
def vector(value,default=(0,0)):
 v=numbers(value);return tuple(v[-2:]) if len(v)>=2 else default

def reference(value):
 m=re.search(r'(Ext|Sub)Resource\("([^"]+)"\)',value);return (m[1],m[2]) if m else ('','')
@functools.lru_cache(None)
def image(path):return Image.open(SOURCE/path).convert('RGBA')
def asset(path,region=None):
 key=path+(':'+str(region) if region else '')
 if key in ASSETS:return ASSETS[key]
 source=SOURCE/path
 if not source.exists():raise FileNotFoundError(source)
 name=hashlib.sha256(key.encode()).hexdigest()[:20];dest=ROOT/'assets'/(name+('.png' if source.suffix.lower() in ('.png','.webp','.jpg','.jpeg') else source.suffix.lower()))
 if not dest.exists():
  if dest.suffix=='.png':
   im=image(path)
   if region:
    x,y,w,h=map(round,region)
    if w<=0 or h<=0 or x<0 or y<0 or x+w>im.width or y+h>im.height:raise ValueError(f'Atlas fora da textura: {path} {region} {im.size}')
    im=im.crop((x,y,x+w,y+h))
   im.save(dest)
  else:dest.write_bytes(source.read_bytes())
 ASSETS[key]='assets/'+dest.name;return ASSETS[key]
@functools.lru_cache(None)
def load(path):return Scene(path)
class Scene:
 def __init__(self,path):
  self.path=path;text=(SOURCE/path).read_text();matches=list(re.finditer(r'^\[([^\n]+)\]\s*$',text,re.M));self.sections=[(m[1],text[m.end():matches[i+1].start() if i+1<len(matches) else len(text)]) for i,m in enumerate(matches)]
  self.ext={attrs(h)['id']:attrs(h) for h,b in self.sections if h.startswith('ext_resource')};self.sub={attrs(h)['id']:(h,b) for h,b in self.sections if h.startswith('sub_resource')};self.root=next((b for h,b in self.sections if h=='resource'),'');self.nodes=[];self.paths={}
  for h,b in self.sections:
   if not h.startswith('node'):continue
   a=attrs(h);a['body']=b;a['header']=h;parent=a.get('parent','');p=a['name'] if not parent else a['name'] if parent=='.' else parent+'/'+a['name'];a['nodepath']=p;self.nodes.append(a);self.paths[p]=a
 def extpath(self,id):return self.ext[id]['path'][6:]
 def resource(self,value):
  typ,id=reference(value)
  if typ=='Ext':return load(self.extpath(id)),load(self.extpath(id)).root
  return self,self.sub[id][1]
 def texture(self,value):
  typ,id=reference(value)
  if typ=='Ext':path=self.extpath(id);im=image(path);return path,(0,0,im.width,im.height)
  if typ=='Sub' and id in self.sub:
   h,b=self.sub[id]
   if 'AtlasTexture' in h:
    path,_=self.texture(field(b,'atlas'));return path,tuple(numbers(field(b,'region'))[-4:])
  return None,None
 def transform(self,node):
  pos=vector(field(node['body'],'position'));scale=vector(field(node['body'],'scale'),(1,1));parent=node.get('parent','')
  if parent not in ('','.'):pp,ps=self.transform(self.paths[parent]);return (pp[0]+pos[0]*ps[0],pp[1]+pos[1]*ps[1]),(ps[0]*scale[0],ps[1]*scale[1])
  return pos,scale
 def node(self,name):return next((n for n in self.nodes if n['name']==name),None)
 def cells(self,node):
  body=node['body'];m=re.search(r'PackedByteArray\("([^"]+)"\)',field(body,'tile_map_data'))
  if not m:return []
  raw=base64.b64decode(m[1]);assert len(raw[2:])%12==0,self.path
  return list(struct.iter_unpack('<hhHHHH',raw[2:]))
 def tileset(self,node):
  owner,body=self.resource(field(node['body'],'tile_set'));result={};cell=vector(field(body,'tile_size','Vector2i(16,16)'))
  for sid,id in re.findall(r'sources/(\d+) = SubResource\("([^"]+)"\)',body):
   h,b=owner.sub[id];tex,region=owner.texture(field(b,'texture'))
   if not tex:continue
   result[int(sid)]={'texture':tex,'size':vector(field(b,'texture_region_size','Vector2i(16,16)')),'margins':vector(field(b,'margins','Vector2i(0,0)')),'separation':vector(field(b,'separation','Vector2i(0,0)')),'body':b}
  return cell,result
 def sprite(self):
  return next((n for n in self.nodes if n.get('type') in ('Sprite2D','AnimatedSprite2D') and n['name'] not in ('Shadow','Gun')),None)
 def visuals(self,node):
  b=node['body'];animations={};initial='idle'
  if node.get('type')=='AnimatedSprite2D':
   owner,body=self.resource(field(b,'sprite_frames'))
   for frames,loop,name,speed in re.findall(r'"frames": \[(.*?)\],\s*"loop": (true|false),\s*"name": &?"([^"]+)",\s*"speed": ('+NUM+')',body,re.S):
    paths=[]
    for value in re.findall(r'"texture": ((?:Ext|Sub)Resource\("[^"]+"\))',frames):
     tex,region=owner.texture(value)
     if tex:paths.append(asset(tex,region))
    if paths:animations[name]={'frames':paths,'fps':max(.1,float(speed)),'loop':loop=='true'}
   initial=field(b,'animation',field(b,'autoplay','"default"')).replace('&','').strip('"')
   if initial not in animations:initial=next(iter(animations),'')
   first=animations[initial]['frames'][0] if initial else '';im=Image.open(ROOT/first) if first else None
   return first,(im.width,im.height) if im else (64,64),animations,initial
  tex,region=self.texture(field(b,'texture'))
  if not tex:return '',(64,64),{},''
  hf=int(field(b,'hframes','1'));vf=int(field(b,'vframes','1'));fw=region[2]/hf;fh=region[3]/vf
  def frame(i):return asset(tex,(region[0]+(i%hf)*fw,region[1]+(i//hf)*fh,fw,fh))
  for name,sid in re.findall(r'"([^"]+)": SubResource\("([^"]+)"\)', '\n'.join(v for h,v in self.sub.values() if 'AnimationLibrary' in h)):
   if sid not in self.sub:continue
   _,animation=self.sub[sid];indices=[]
   for track in re.findall(r'tracks/(\d+)/path = NodePath\("[^"\n]*:frame"\)',animation):
    m=re.search(r'tracks/'+track+r'/keys = \{(.*?)\n\}',animation,re.S)
    if m:
     values=re.search(r'"values": \[([^\]]+)\]',m[1]);indices=[int(v) for v in re.findall(r'\d+',values[1])] if values else []
   if indices:animations[name]={'frames':[frame(i) for i in indices if i<hf*vf],'fps':max(1,len(indices)/float(field(animation,'length','1'))),'loop':field(animation,'loop_mode','0')=='1'}
  initial=next((n for n in ['idle','walk','run','default'] if n in animations),next(iter(animations),''));fr=int(field(b,'frame','0'));static=frame(min(fr,hf*vf-1));return static,(fw,fh),animations,initial
 def collider(self,scale):
  n=next((n for n in self.nodes if n.get('type')=='CollisionShape2D' and n.get('parent')=='.' and field(n['body'],'disabled')!='true'),None)
  if not n:return None
  owner,b=self.resource(field(n['body'],'shape'));size=vector(field(b,'size'))
  if size==(0,0):radius=float(field(b,'radius','16'));height=float(field(b,'height',str(radius*2)));size=(radius*2,height)
  cs=vector(field(n['body'],'scale'),(1,1));size=(size[0]*abs(cs[0]),size[1]*abs(cs[1]))
  rot=float(field(n['body'],'rotation','0'))
  if abs(abs(rot)-math.pi/2)<.02:size=(size[1],size[0])
  pos=vector(field(n['body'],'position'));return pos,size

def identifier(path):return str(uuid.uuid5(uuid.NAMESPACE_URL,'final-roz:'+path))
@functools.lru_cache(maxsize=8192)
def polygons(body,ax,ay,alt):
 return [numbers(m) for m in re.findall(rf'{ax}:{ay}/{alt}/physics_layer_0/polygon_\d+/points = PackedVector2Array\(([^)]+)\)',body)]
def atlas(source,ax,ay,alt):
 return cached_atlas(tuple(source['size']),tuple(source['margins']),tuple(source['separation']),source['body'],ax,ay,alt)
@functools.lru_cache(maxsize=8192)
def cached_atlas(size,margins,separation,body,ax,ay,alt):
 w,h=size;mx,my=margins;sx,sy=separation;tx=mx+ax*(w+sx);ty=my+ay*(h+sy)
 multi=re.search(rf'^{ax}:{ay}/size_in_atlas = Vector2i\(([^)]+)\)',body,re.M)
 if multi:mw,mh=vector(multi[1]);w=w*mw+sx*(mw-1);h=h*mh+sy*(mh-1)
 origin=re.search(rf'^{ax}:{ay}/{alt}/texture_origin = Vector2i\(([^)]+)\)',body,re.M);origin=vector(origin[1]) if origin else (0,0)
 return (tx,ty,w,h),origin

def obstacle(points):
 xs=points[::2];ys=points[1::2];x=min(xs);y=min(ys);w=max(xs)-x;h=max(ys)-y
 if w<1e-6 or h<1e-6:return None
 poly=[v-(x if i%2==0 else y) for i,v in enumerate(points)]
 rect=all(abs(a-x)<1e-6 or abs(a-x-w)<1e-6 for a in xs) and all(abs(a-y)<1e-6 or abs(a-y-h)<1e-6 for a in ys)
 return {'x':x,'y':y,'width':w,'height':h,'colliderPolygon':[] if rect else poly}
def merge_rectangles(obstacles):
 groups=collections.defaultdict(list);rest=[]
 for e in obstacles:
  if e['colliderPolygon']:rest.append(e)
  else:groups[(round(e['y'],5),round(e['height'],5))].append(e)
 for group in groups.values():
  current=None
  for e in sorted(group,key=lambda e:e['x']):
   if current and abs(current['x']+current['width']-e['x'])<.0001:current['width']+=e['width']
   else:
    if current:rest.append(current)
    current=e.copy()
  if current:rest.append(current)
 return rest
