"""Remove the original full-cell collision from the decorative grass fringe."""
GRASS_ATLAS='assets/78cff35fac9d9875eb7f.png'
def fix_grass(game):
 count=0
 for scene in game['scenes']:
  for tilemap in scene.get('tileMaps',[]):
   ids={int(key) for key,tile in tilemap['tiles'].items() if tile['assetPath']==GRASS_ATLAS and tile.get('sourceX')==128 and tile.get('sourceY')==256}
   if not ids:continue
   for layer in tilemap['layers']:
    for index,tile in enumerate(layer['data']):
     if tile not in ids:continue
     x=index%tilemap['columns']*tilemap['tileWidth'];y=(tilemap['rows']-1-index//tilemap['columns'])*tilemap['tileHeight']
     right=x+tilemap['tileWidth'];top=y+tilemap['tileHeight'];result=[]
     for obstacle in tilemap['obstacles']:
      ox=obstacle['x'];oy=obstacle['y'];orr=ox+obstacle['width'];ot=oy+obstacle['height']
      if ox>=right or orr<=x or oy>=top or ot<=y:result.append(obstacle);continue
      if obstacle.get('colliderPolygon'):raise ValueError('Unexpected polygon overlapping decorative grass')
      ix=max(x,ox);iy=max(y,oy);ir=min(right,orr);it=min(top,ot)
      for px,py,w,h in [(ox,oy,orr-ox,iy-oy),(ox,it,orr-ox,ot-it),(ox,iy,ix-ox,it-iy),(ir,iy,orr-ir,it-iy)]:
       if w>0 and h>0:result.append(dict(obstacle,x=px,y=py,width=w,height=h))
     tilemap['obstacles']=result;count+=1
 return count
