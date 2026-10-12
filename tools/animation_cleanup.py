"""Moderate imported actor animation cadence without changing movement speed."""
def moderate_actor_animations(game):
 changed=0
 for scene in game['scenes']:
  for entity in scene['objects']:
   if entity.get('properties',{}).get('kind') not in ('enemy','npc'):continue
   for animation in entity.get('animations',{}).values():
    if animation.get('loop') and len(animation.get('frames',[]))>1 and animation['fps']>8:
     animation['fps']=8;changed+=1
 return changed
