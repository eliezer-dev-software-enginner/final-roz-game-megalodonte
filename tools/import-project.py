#!/usr/bin/env python3
"""Reproducible import of all 17 Final Roz levels into portable Megalodonte data."""
import collections,json,math,re,shutil,sys
from pathlib import Path
from godot_import import *
if (ROOT/'game.json').exists() and '--force' not in sys.argv:raise SystemExit('Use --force para regenerar; edições existentes devem ser preservadas antes.')
(ROOT/'assets').mkdir(exist_ok=True)
LEVELS=['level/level_1_neu.tscn','level/level_2_neu.tscn']+[f'level/level_{i}.tscn' for i in range(3,15)]+['level/level_20.tscn','isometricRoz/dungeon.tscn','level/level_15.tscn']
reports=[];scenes=[]
def hud(id,text,x,y,w=240,h=55,action='',imagepath='',anchor='left',vertical='top'):
 return {'id':id,'type':'image' if imagepath and not action else 'button' if action else 'text','text':text,'assetPath':imagepath,'offsetX':x,'offsetY':y,'width':w,'height':h,'fontSize':24,'action':action,'horizontalAnchor':anchor,'verticalAnchor':vertical}
def kind(path):
 name=Path(path).stem
 if path.startswith('player/') or '/player/' in path:return 'player'
 if name=='coin':return 'coin'
 if 'schluessel' in name:return 'key'
 if name.startswith('tuer'):return 'door'
 if 'passant' in name:return 'npc'
 if path.startswith('enemy/') or 'gegner' in path:return 'enemy'
 if 'platform' in path:return 'platform'
 return 'decoration'
def color_key(path):return 'blue' if 'blau' in path else 'green' if 'gruen' in path else 'red'
def collision(e,owner,node,position,scale,convert):
 shape=owner.collider(scale)
 if not shape:return
 pos,size=shape;cx=position[0]+pos[0]*scale[0];cy=position[1]+pos[1]*scale[1];w=abs(size[0]*scale[0])/64;h=abs(size[1]*scale[1])/64
 x,y=convert(cx-abs(size[0]*scale[0])/2,cy+abs(size[1]*scale[1])/2)
 e.update(colliderOffsetX=x-e['x'],colliderOffsetY=y-e['y'],colliderWidth=w,colliderHeight=h)
def entity(owner,node,position,scale,convert,nodepath,classification='decoration'):
 texture,size,animations,initial=owner.visuals(node)
 if not texture:return None
 spritepos,spritescale=owner.transform(node);w=abs(size[0]*spritescale[0]*scale[0]);h=abs(size[1]*spritescale[1]*scale[1]);px=position[0]+spritepos[0]*scale[0];py=position[1]+spritepos[1]*scale[1]
 centered=field(node['body'],'centered','true')!='false'
 if not centered:px+=w/2;py+=h/2
 x,y=convert(px-w/2,py+h/2)
 e={'id':identifier(nodepath),'name':nodepath.split('/')[-1],'assetPath':texture,'x':x,'y':y,'width':max(.001,w/64),'height':max(.001,h/64),'visible':True,'solid':classification in ('player','enemy','npc','door','coin','key','platform'),'trigger':classification in ('player','enemy','npc','coin','key'),'colliderFollowsFlip':False,'flipX':scale[0]<0,'properties':{'kind':classification,'source':owner.path,'node':nodepath},'animations':animations,'animation':initial}
 if classification!='decoration':collision(e,owner,node,position,scale,convert)
 if classification=='player':e['id']='player';e['name']='Roz';e['scriptClassName']='game.finalroz.Player'
 elif classification in ('enemy','npc'):
  e['scriptClassName']='game.finalroz.Enemy';script=next((owner.extpath(id) for id,a in owner.ext.items() if a['type']=='Script'),'');body=(SOURCE/script).read_text() if script else '';speed=re.search(r'(?:WALK_SPEED|MOTION_SPEED)\s*=\s*('+NUM+')',body)
  e['properties'].update(speed=str(float(speed[1])/64 if speed else 2),flying=str('gravity' not in body).lower(),hp='1',harmless=str(classification=='npc').lower())
 elif classification in ('coin','key'):e['scriptClassName']='game.finalroz.Collectible'
 elif classification=='door':e['scriptClassName']='game.finalroz.Door'
 if classification in ('key','door'):e['properties']['keyColor']=color_key(owner.path)
 if classification=='platform':e['scriptClassName']='game.finalroz.Platform'
 return e

def build_level(path,index):
 source=load(path);iso=index==16;layers=[];bounds=[];objects=[];unhandled=[]
 for n in source.nodes:
  if n.get('type')!='TileMapLayer':continue
  cell,sources=source.tileset(n);pos,scale=source.transform(n);cells=source.cells(n);layers.append((n,cell,sources,pos,scale,cells))
  for x,y,sid,ax,ay,alt in cells:
   if iso:cx=(x-y)*cell[0]/2;cy=(x+y)*cell[1]/2
   else:cx=(x+.5)*cell[0];cy=(y+.5)*cell[1]
   bounds.extend([(pos[0]+cx*scale[0]-cell[0]*abs(scale[0]),pos[1]+cy*scale[1]-cell[1]*abs(scale[1])),(pos[0]+cx*scale[0]+cell[0]*abs(scale[0]),pos[1]+cy*scale[1]+cell[1]*abs(scale[1]))])
 for n in source.nodes:
  if 'parent' in n:position,scale=source.transform(n);bounds.append(position)
 minx=math.floor(min(x for x,y in bounds)/64)*64-512;maxx=math.ceil(max(x for x,y in bounds)/64)*64+512;miny=math.floor(min(y for x,y in bounds)/64)*64-512;maxy=math.ceil(max(y for x,y in bounds)/64)*64+512
 def convert(x,y):return ((x-minx)/64,(maxy-y)/64)
 scene={'id':f'level-{index}','name':f'Fase {index}'+(' • isométrica' if iso else ''),'background':'#1d2146','worldWidth':(maxx-minx)/64,'worldHeight':(maxy-miny)/64,'depthSort':iso,'objects':objects,'tileMaps':[],'hud':[],'musicAssetPath':asset('music.ogg'),'musicVolume':.55,'scriptClassName':'game.finalroz.Session'}
 for n in source.nodes:
  if 'instance=ExtResource' not in n['header']:continue
  match=re.search(r'instance=ExtResource\("([^"]+)"',n['header']);rel=source.extpath(match[1])
  if 'parallax_background' in rel:
   backdrop=load(rel);sprite=backdrop.sprite()
   if sprite:
    texture,region=backdrop.texture(field(sprite['body'],'texture'));scene['backgroundAssetPath']=asset(texture,region);scene['backgroundParallaxX']=.2
 for layer_index,(n,cell,sources,pos,scale,cells) in enumerate(layers):
  if not cells:continue
  if iso:
   for ci,(x,y,sid,ax,ay,alt) in enumerate(cells):
    if sid not in sources:unhandled.append(f'tile source {sid}');continue
    src=sources[sid];region,origin=atlas(src,ax,ay,alt);tex=asset(src['texture'],region);w=region[2]*abs(scale[0]);h=region[3]*abs(scale[1]);cx=pos[0]+((x-y)*cell[0]/2-origin[0])*scale[0];cy=pos[1]+((x+y)*cell[1]/2-origin[1])*scale[1];wx,wy=convert(cx-w/2,cy+h/2)
    e={'id':identifier(path+'/'+n['name']+'/'+str(ci)),'name':n['name']+' '+str(ci),'assetPath':tex,'x':wx,'y':wy,'width':w/64,'height':h/64,'solid':False,'visible':True,'drawLayer':-1 if 'Floor' in n['name'] else 0,'properties':{'kind':'iso-floor' if 'Floor' in n['name'] else 'iso-wall','source':path,'layer':n['name']}}
    polys=polygons(src['body'],ax,ay,alt)
    if polys and field(n['body'],'collision_enabled','true')!='false':
     pts=polys[0];world=[]
     for j in range(0,len(pts),2):xx,yy=convert(cx+pts[j]*scale[0],cy+pts[j+1]*scale[1]);world.extend([xx,yy])
     geom=obstacle(world)
     if geom:
      e.update(solid=True,colliderOffsetX=geom['x']-wx,colliderOffsetY=geom['y']-wy,colliderWidth=geom['width'],colliderHeight=geom['height'],colliderPolygon=[v-(wx if j%2==0 else wy) for j,v in enumerate(world)])
    else:e.update(colliderOffsetY=0,colliderWidth=w/64,colliderHeight=.01)
    objects.append(e)
   continue
  xs=[c[0] for c in cells];ys=[c[1] for c in cells];left=min(xs);top=min(ys);right=max(xs)+1;bottom=max(ys)+1;cols=right-left;rows=bottom-top;tiles={};keys={};data=[0]*(cols*rows);obstacles=[]
  assert abs(scale[0]-scale[1])<1e-6,(path,n['name'],'nonuniform map scale')
  for x,y,sid,ax,ay,alt in cells:
   if sid not in sources:unhandled.append(f'tile source {sid}');continue
   src=sources[sid];region,origin=atlas(src,ax,ay,alt);key=(sid,ax,ay,alt)
   if key not in keys:
    tid=len(keys)+1;keys[key]=tid;tiles[str(tid)]={'assetPath':asset(src['texture']),'sourceX':int(region[0]),'sourceY':int(region[1]),'width':int(region[2]),'height':int(region[3])}
   data[(y-top)*cols+x-left]=keys[key]
   if field(n['body'],'collision_enabled','true')!='false':
    for points in polygons(src['body'],ax,ay,alt):
     local=[]
     for j in range(0,len(points),2):local.extend([(x-left)*cell[0]+cell[0]/2+points[j],(bottom-y-1)*cell[1]+cell[1]/2-points[j+1]])
     geom=obstacle(local)
     if geom:obstacles.append(geom)
  mx,my=convert(pos[0]+left*cell[0]*scale[0],pos[1]+bottom*cell[1]*scale[1])
  scene['tileMaps'].append({'id':identifier(path+'/'+n['name']),'name':n['name'],'columns':cols,'rows':rows,'tileWidth':int(cell[0]),'tileHeight':int(cell[1]),'unitScale':scale[0]/64,'x':mx,'y':my,'tiles':tiles,'layers':[{'name':n['name'],'data':data}],'obstacles':merge_rectangles(obstacles)})
 for n in source.nodes:
  header=n['header'];pos,scale=source.transform(n)
  if 'instance=ExtResource' in header:
   id=re.search(r'instance=ExtResource\("([^"]+)"',header)[1];rel=source.extpath(id);category=kind(rel)
   if category=='decoration':continue
   template=load(rel);sprite=template.sprite()
   if not sprite:unhandled.append(rel+': missing sprite');continue
   rootscale=vector(field(template.nodes[0]['body'],'scale'),(1,1));effective=(scale[0]*rootscale[0],scale[1]*rootscale[1]);e=entity(template,sprite,pos,effective,convert,path+'/'+n['nodepath'],category)
   if e:
    if category=='player':e['properties'].update(mode='isometric' if iso else 'platform',level=str(index),advanced=str(index in (15,17)).lower())
    if category=='enemy' and iso:e['properties']['isometric']='true'
    objects.append(e)
  elif n.get('type') in ('Sprite2D','AnimatedSprite2D') and '/AnimatableBody' not in n['nodepath']:
   # Parent transforms already contain the sprite scale; avoid applying them twice.
   parent=n.get('parent','.');parentpos,parentscale=source.transform(source.paths[parent]) if parent not in ('','.') else ((0,0),(1,1))
   # For a level-owned sprite, entity computes its complete local transform.
   e=entity(source,n,(0,0),(1,1),convert,path+'/'+n['nodepath'])
   if e:objects.append(e)
  elif n.get('type')=='AnimatableBody2D':
   sprite=next((c for c in source.nodes if c.get('parent')==n['nodepath'] and c.get('type')=='Sprite2D'),None)
   if not sprite:unhandled.append(n['nodepath']);continue
   e=entity(source,sprite,(0,0),(1,1),convert,path+'/'+n['nodepath'],'platform')
   col=next((c for c in source.nodes if c.get('parent')==n['nodepath'] and c.get('type')=='CollisionShape2D'),None)
   if col:
    owner,b=source.resource(field(col['body'],'shape'));w,h=vector(field(b,'size'));cp,cs=source.transform(col);wx,wy=convert(cp[0]-w*abs(cs[0])/2,cp[1]+h*abs(cs[1])/2);e.update(colliderOffsetX=wx-e['x'],colliderOffsetY=wy-e['y'],colliderWidth=w*abs(cs[0])/64,colliderHeight=h*abs(cs[1])/64)
   anim=next((c for c in source.nodes if c.get('parent')==n['nodepath'] and c.get('type')=='AnimationPlayer'),None)
   if anim:
    name=field(anim['body'],'autoplay').strip('"');trackbody=next((b for h,b in source.sub.values() if 'type="Animation"' in h and field(b,'resource_name').strip('"')==name),'')
    match=re.search(r'tracks/0/keys = \{(.*?)\n\}',trackbody,re.S)
    if match:
     times=re.search(r'"times": PackedFloat32Array\(([^)]+)\)',match[1]);points=re.findall(r'Vector2\(([^)]+)\)',match[1]);position=vector(field(n['body'],'position'));parent=n.get('parent','.');pp,ps=source.transform(source.paths[parent]) if parent!='.' else ((0,0),(1,1));route=[]
     for t,point in zip(numbers(times[1]) if times else [],points):
      px,py=vector(point);route.append([t,e['x']+(px-position[0])*ps[0]/64,e['y']-(py-position[1])*ps[1]/64])
     if route:e['properties']['route']=json.dumps(route,separators=(',',':'))
   objects.append(e)
  elif n.get('type')=='CollisionShape2D' and 'swim' in n.get('parent','').lower():
   owner,b=source.resource(field(n['body'],'shape'));w,h=vector(field(b,'size'));wx,wy=convert(pos[0]-w*abs(scale[0])/2,pos[1]+h*abs(scale[1])/2);objects.append({'id':identifier(path+'/'+n['nodepath']),'name':'Água • natação','x':wx,'y':wy,'width':w*abs(scale[0])/64,'height':h*abs(scale[1])/64,'visible':False,'solid':True,'trigger':True,'properties':{'kind':'water'}})
  elif n.get('type')=='CollisionShape2D' and n.get('parent') in source.paths and source.paths[n['parent']].get('type')=='StaticBody2D':
   owner,b=source.resource(field(n['body'],'shape'));w,h=vector(field(b,'size'));wx,wy=convert(pos[0]-w*abs(scale[0])/2,pos[1]+h*abs(scale[1])/2);objects.append({'id':identifier(path+'/'+n['nodepath']),'name':'Parede '+n['name'],'x':wx,'y':wy,'width':w*abs(scale[0])/64,'height':h*abs(scale[1])/64,'visible':False,'solid':True,'properties':{'kind':'wall'}})
 player=next(e for e in objects if e['id']=='player');scene['camera']={'mode':'follow','targetId':'player','x':player['x']+player['width']/2,'y':player['y']+player['height']/2,'width':30 if not iso else 38,'height':16.875 if not iso else 21.375,'followSpeed':8,'deadZoneWidth':6,'deadZoneHeight':2,'holdGroundDuringJump':not iso,'fadeSeconds':.35}
 scene['hud']=[hud('status','',20,15,1000,60),hud('pause','Ⅱ',15,10,90,70,'pause',anchor='right'),hud('menu','MENU',120,10,160,60,'menu',anchor='right'),hud('left','◀',25,15,145,135,'left',vertical='bottom',imagepath=asset('gui/touch_button_left.webp')),hud('right','▶',190,15,145,135,'right',vertical='bottom',imagepath=asset('gui/touch_button_right.webp')),hud('jump','↑',25,15,145,135,'jump' if not iso else 'up',anchor='right',vertical='bottom',imagepath=asset('gui/touch_button_jump.webp')),hud('shoot','TIRO' if not iso else 'BOMBA',190,15,145,135,'shoot',anchor='right',vertical='bottom',imagepath=asset('gui/touch_button_fire.webp')),hud('crouch','↓',375,15,145,135,'crouch' if not iso else 'down',vertical='bottom',imagepath=asset('gui/touch_button_down.png')),hud('run','CORRER',550,15,160,95,'run',vertical='bottom'),hud('message','',320,130,680,80),hud('resume','CONTINUAR',460,290,360,75,'resume'),hud('retry','REINICIAR',460,390,360,75,'retry')]
 for h in scene['hud']:
  if h['id'] in ('message','resume','retry'):h['visible']=False
  if h['assetPath']:h['text']=''
  if h['id']=='run' and index not in (15,17):h['visible']=False
  if h['id']=='crouch' and index not in (15,16,17):h['visible']=False
 report={'level':index,'source':path,'scene':scene['id'],'tiles':sum(len(l[-1]) for l in layers),'objects':len(objects),'coins':sum(e['properties'].get('kind')=='coin' for e in objects),'enemies':sum(e['properties'].get('kind')=='enemy' for e in objects),'polygons':sum(bool(o['colliderPolygon']) for m in scene['tileMaps'] for o in m['obstacles']),'unhandled':unhandled};reports.append(report);print(report['scene'],report['tiles'],report['objects'],report['coins'],'issues',unhandled[:4]);return scene

for i,path in enumerate(LEVELS,1):scenes.append(build_level(path,i))
for path in ['player/jump.wav','player/shoot.wav','player/ouch.wav','player/coin_pickup.wav','player/creakydoor4.wav','isometricRoz/bomb/explosion-9-340460.wav','isometricRoz/bomb/bomb.png']:
 if (SOURCE/path).exists():asset(path)
# Weapon templates and sound references remain authored data visible in the editor.
for scene in scenes:
 player=next(e for e in scene['objects'] if e['id']=='player')
 for key,path in {'jumpSound':'player/jump.wav','shootSound':'player/shoot.wav','hurtSound':'player/ouch.wav','coinSound':'player/coin_pickup.wav','doorSound':'player/creakydoor4.wav','bombSound':'isometricRoz/bomb/explosion-9-340460.wav'}.items():player['properties'][key]=asset(path)
 for id,path,kindname,width,height in [('bullet-template','player/bullet.tscn','bullet',.55,.22),('bomb-template','isometricRoz/bomb/bomb.tscn','bomb',1.0,1.0)]:
  owner=load(path);node=owner.sprite();texture,size,animations,initial=owner.visuals(node)
  rootscale=vector(field(owner.nodes[0]['body'],'scale'),(1,1));e=entity(owner,node,(0,0),rootscale,lambda x,y:(x/64,-y/64),path,'template');e.update(id=id,name='Modelo '+kindname,x=player['x'],y=player['y'],solid=False,visible=False,trigger=True);e['properties']['projectileType']=kindname;scene['objects'].append(e)
def screen(id,name,background='#000000'):
 return {'id':id,'name':name,'background':background,'worldWidth':30,'worldHeight':16.875,'camera':{'x':15,'y':8.4375,'width':30,'height':16.875},'scriptClassName':'game.finalroz.Menu','musicAssetPath':asset('music.ogg'),'musicVolume':.35,'musicLoop':True,'objects':[],'hud':[]}
def original_button(h):
 h.update(referenceArea='content',buttonBackground='#222222',buttonOpacity=.7,cornerRadius=0,fontFamily='SansSerif',textAlign='center',textVerticalAlign='center',fontSize=66.6667);return h
def original_screen(path,id,name,action):
 source=load(path);result=screen(id,name);sprite=source.sprite();texture,region=source.texture(field(sprite['body'],'texture'))
 h=hud('art','',0,0,1280,720,imagepath=asset(texture,region));h['referenceArea']='content';result['hud'].append(h)
 button=next((n for n in source.nodes if n.get('type')=='Button'),None)
 if button:
  b=button['body'];left=float(field(b,'offset_left','0'));top=float(field(b,'offset_top','0'));right=float(field(b,'offset_right','335'));bottom=float(field(b,'offset_bottom','145'));text=field(b,'text','"START!"').strip('"');result['hud'].append(original_button(hud('start' if action.startswith('start:') else 'continue',text,(left+960)*2/3,(top+540)*2/3,(right-left)*2/3,(bottom-top)*2/3,action)))
  result['hud'][-1]['fontSize']=60
 return result
menu=screen('menu','Menu • Final Roz')
menu['hud']=[dict(hud('title','',0,0,1280,720,imagepath=asset('rozGRAFX/roz game images/RoZWareGameTitle.png')),referenceArea='content'),original_button(hud('records','Beste Zeit',19.333,471.333,342,110,'records')),original_button(hud('play','Play',18.667,588.667,196.667,110,'play'))]
menu['hud'][1],menu['hud'][2]=menu['hud'][2],menu['hud'][1]
menu['hud'] += [dict(original_button(hud('options','☰',25,20,75,65,'options',anchor='right')),fontSize=32),dict(hud('message','',350,630,860,60),referenceArea='content')]
options=screen('options','Menu • opções e dados salvos')
options['hud']=[hud('heading','FINAL ROZ • OPÇÕES',100,40,1080,90),hud('levels','SELECIONAR FASE',380,150,520,65,'levels'),hud('export','EXPORTAR JSON',380,235,520,65,'export'),hud('import','IMPORTAR JSON',380,320,520,65,'import'),hud('audio','ÁUDIO: ON',380,405,520,65,'audio'),hud('touch','TOQUE: ON',380,490,520,65,'touch'),hud('help','CONTROLES',380,575,520,65,'help'),hud('back','VOLTAR',30,625,260,70,'back'),hud('message','',100,90,1080,55)]
help_scene=screen('help','Ajuda • controles')
help_scene['hud']=[hud('heading','CONTROLES',90,60,1100,75),hud('instructions','A/D ou setas: mover • Espaço: salto duplo • X ou Ctrl: atirar\nFases 15 e 17: Shift para correr; baixo para agachar\nÁgua da fase 15: direcional para nadar; salto sobe\nFase 16: mover em 8 direções; X coloca uma bomba\nGamepad: A pula, X atira, R1 corre, Start pausa, B volta\nColete todas as moedas para avançar. Chaves abrem portas.\nPedestres são inofensivos. Evite inimigos e explosões.',90,190,1100,370),hud('back','VOLTAR',30,625,260,70,'options'),hud('message','',350,625,860,60)]
select=screen('levels','Seleção de fase');select['hud']=[hud('heading','FINAL ROZ • SELECIONE A FASE',60,30,1100),hud('back','VOLTAR',40,610,220,80,'options'),hud('message','',40,545,1100,55)]
for i in range(1,18):
 h=hud('level-'+str(i),str(i),160+(i-1)%6*165,150+(i-1)//6*120,140,95,'level:'+str(i));h['properties']={'liberado':i==1};h['condition']={'property':'liberado','disableWhenFalse':True};select['hud'].append(h)
records=screen('records','Recordes');records['hud']=[hud('heading','MELHORES TEMPOS',60,25,1100),hud('back','VOLTAR',40,625,200,65,'back'),hud('message','',260,630,950,60)]
for i in range(1,18):records['hud'].append(hud('record-'+str(i),'',65+(i-1)//9*620,100+(i-1)%9*53,570,50))
# Original story screens are independent editor scenes; filenames 12/15 precede phases 15/17.
story_sources={1:1,2:2,5:5,7:7,9:9,11:11,15:12,17:15}
stories=[original_screen(f'level/level_{source}_info.tscn',f'story-{level}',f'História • fase {level}','card:'+str(level)) for level,source in story_sources.items()]
intros=[]
for i in range(1,18):
 if i<17:intro=original_screen(f'level/level_{i}_start.tscn','intro-'+str(i),f'Abertura • LEVEL {i}','start:'+str(i))
 else:
  intro=screen('intro-17','Abertura • LEVEL 17');intro['hud']=[hud('number','17',550,40,240,200),hud('level','LEVEL',240,260,900,270),original_button(hud('start','START!',980,560,220,100,'start:17'))];intro['hud'][0].update(fontSize=180,textAlign='center');intro['hud'][1].update(fontSize=180,color='#4fae43',textAlign='center')
 intro['hud'].append(hud('message','',30,15,900,55));intros.append(intro)
ending=screen('ending','Final • jogo concluído');ending['hud']=[hud('heading','FINAL ROZ • TODAS AS FASES CONCLUÍDAS',90,160,1150,100),hud('credits','Christian Röttger • RozWareProjekte\nAdaptação Megalodonte Engine • licença MIT',190,310,950,160),hud('records','RECORDES',450,500,380,85,'records'),hud('back','MENU',40,600,240,85,'back'),hud('message','',350,620,860,60)]
# Menu composition uses the same centered 16:9 reference as the original camera.
for ui_scene in [menu,options,help_scene,select,records]+stories+intros+[ending]:
 for h in ui_scene['hud']:h['referenceArea']='content'
# Settings and FPS overlay remain authorable and survive regenerating the source project.
option_huds={h['id']:h for h in options['hud']}
for key,label in [('showFps','MOSTRAR FPS: OFF'),('soundVolume','EFEITOS: 35%')]:
 options['hud'].append(hud(key,label,100,425,520,65,key))
option_huds={h['id']:h for h in options['hud']}
for key,(x,y,w) in {'levels':(380,150,520),'export':(100,245,520),'import':(660,245,520),'audio':(100,335,520),'touch':(660,335,520),'showFps':(100,425,520),'soundVolume':(660,425,520),'help':(380,515,520)}.items():
 option_huds[key].update(offsetX=x,offsetY=y,width=w,height=65)
option_huds['message'].update(offsetX=320,offsetY=605,width=900,height=50,fontSize=22)
for scene in [menu,options,help_scene,select,records]+stories+intros+scenes+[ending]:
 gameplay=scene['id'].startswith('level-')
 scene['hud'].append(dict(hud('fps','FPS: —',20 if gameplay else 110,80 if gameplay else 8,140,28,anchor='right'),fontSize=18,color='#b8c8d8',textAlign='right',visible=False,scriptClassName='game.finalroz.FpsOverlay',referenceArea='viewport'))
# Native orientation is data, independent of patrol direction and animation order.
left_facing={{'hai.tscn', 'zombie.tscn', 'scorpion.tscn', 'frankenstein.tscn', 'schlange.tscn', 'ant.tscn', 'passantin.tscn', 'kroko.tscn', 'wizard.tscn', 'welly.tscn', 'alienpassant.tscn', 'nosferatu.tscn', 'passant.tscn', 'kleinerdrache.tscn'}}
for scene in scenes:
 for entity in scene['objects']:
  if entity.get('scriptClassName') in ('game.finalroz.Enemy','game.finalroz.Player'):
   props=entity['properties'];props['spriteFacing']='left' if Path(props.get('source','')).name in left_facing else 'right'
   if entity['scriptClassName'].endswith('Enemy') and not scene.get('depthSort',False):
    props['patrolDirection']='-1' if entity.get('flipX',False) else '1'
    entity['flipX']=(float(props['patrolDirection'])<0)!=(props['spriteFacing']=='left')
# Keep reviewed, collision-safe spawn poses visible in the editor after regeneration.
corrections_path=ROOT/'docs/spawn-corrections.json'
if corrections_path.exists():
 fixes={(item['scene'],item['id']):item for item in json.loads(corrections_path.read_text())}
 for scene in scenes:
  for entity in scene['objects']:
   correction=fixes.get((scene['id'],entity['id']))
   if correction and abs(entity['x']-correction['sourceX'])<1e-6 and abs(entity['y']-correction['sourceY'])<1e-6:
    entity.update(x=correction['x'],y=correction['y'])
game={'version':1,'name':'Final Roz Game — Megalodonte','applicationId':'megalodonte.games.finalroz','versionCode':1,'versionName':'0.2.0','entrySceneId':'menu','lastEditedSceneId':'menu','iconAssetPath':asset('icon2.png'),'scenes':[menu,options,help_scene,select,records]+stories+intros+scenes+[ending]}
from terrain_cleanup import fix_grass
fix_grass(game)
(ROOT/'game.json').write_text(json.dumps(game,ensure_ascii=False,separators=(",",":"))+'\n');(ROOT/'docs/import-report.json').write_text(json.dumps({'levels':reports,'assets':ASSETS,'warnings':WARNINGS,'policy':'original tile polygons; 17 level scenes; individual actor scripts'},ensure_ascii=False,indent=2)+'\n');shutil.copy2(SOURCE/'LICENSE',ROOT/'LICENSE')
print('17 fases importadas, cenas totais:',len(game['scenes']))
