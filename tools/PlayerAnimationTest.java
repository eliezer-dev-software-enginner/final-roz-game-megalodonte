import megalodonte.engine.*;
import java.nio.file.*;
import java.util.*;
class PlayerAnimationTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static megalodonte.engine.Runtime create(Game.Entity imported,boolean advanced){
  Game g=Game.demo();var s=g.scenes.get(0);s.objects.clear();s.scriptClassName="";s.worldWidth=100;s.worldHeight=30;
  var player=imported.copy();player.x=10;player.y=1-player.colliderOffsetY;player.properties.put("advanced",""+advanced);player.properties.put("invincible","1000");s.objects.add(player);
  var floor=new Game.Entity();floor.x=0;floor.y=0;floor.width=100;floor.height=1;s.objects.add(floor);
  var bullet=new Game.Entity();bullet.id="bullet-template";bullet.visible=false;bullet.solid=false;s.objects.add(bullet);
  return new megalodonte.engine.Runtime(g,name->name.endsWith("Player")?megalodonte.engine.Runtime.reflection(PlayerAnimationTest.class.getClassLoader()).create(name):new GameScript(){},new megalodonte.engine.Runtime.Events(){});
 }
 public static void main(String[] args)throws Exception{
  Game source;try(var reader=Files.newBufferedReader(Path.of("game.json"))){source=Codec.read(reader);}
  var player=source.scenes.stream().filter(s->s.id.equals("level-1")).findFirst().orElseThrow().objects.stream().filter(e->e.id.equals("player")).findFirst().orElseThrow();
  for(String action:List.of("shoot","x","control","pad_x"))try(var r=create(player,false)){
   r.update(.016,0,0);r.setAction(action,true);r.update(.016,0,0);check(r.scene.objects.get(0).animation.equals("idle_weapon"),"Idle gun pose for "+action);
   r.setAction(action,false);for(int i=0;i<8;i++){r.update(.016,0,0);check(r.scene.objects.get(0).animation.equals("idle_weapon"),"Shot pose must survive locomotion updates");}
   for(int i=0;i<10;i++)r.update(.016,0,0);check(r.scene.objects.get(0).animation.equals("idle"),"Return to idle");
  }
  try(var r=create(player,false)){
   r.update(.016,1,0);r.setAction("shoot",true);r.update(.016,1,0);check(r.scene.objects.get(0).animation.equals("run_weapon"),"Run with gun");
  }
  try(var r=create(player,false)){
   r.setAction("space",true);r.setAction("shoot",true);r.update(.016,0,0);check(r.scene.objects.get(0).animation.equals("jumping_weapon"),"Jump with gun");
  }
  try(var r=create(player,false)){
   r.scene.objects.get(0).y=15;r.setAction("shoot",true);r.update(.016,0,0);check(r.scene.objects.get(0).animation.equals("falling_weapon"),"Fall with gun");
  }
  try(var r=create(player,true)){
   r.setAction("crouch",true);r.update(.016,0,0);check(r.scene.objects.get(0).animation.equals("crouch"),"Crouch idle");
   r.update(.016,1,0);check(r.scene.objects.get(0).animation.equals("crouching"),"Crouch walking");
  }
  try(var r=create(player,true)){
   r.update(.016,1,0);r.setAction("shoot",true);r.update(.016,1,0);check(r.scene.objects.get(0).animation.equals("run_weapon"),"Walking gun fallback");
  }
  try(var r=create(player,true)){
   r.setAction("crouch",true);r.update(.016,0,0);var actor=r.scene.objects.get(0);double shortHeight=actor.colliderHeight;
   var bounds=megalodonte.engine.Runtime.collisionBounds(r.scene,actor);var ceiling=new Game.Entity();ceiling.x=bounds.x();ceiling.y=bounds.y()+shortHeight+.05;ceiling.width=bounds.width();ceiling.height=1;r.scene.objects.add(ceiling);
   r.setAction("crouch",false);r.update(.016,0,0);check(actor.colliderHeight==shortHeight&&actor.animation.equals("crouch"),"Remain crouched under low ceiling");
   ceiling.solid=false;r.update(.016,0,0);check(actor.colliderHeight==player.colliderHeight&&actor.animation.equals("idle"),"Stand when clear");
  }
  try(var r=create(player,false)){
   var actor=r.scene.objects.get(0);var bounds=megalodonte.engine.Runtime.collisionBounds(r.scene,actor);var wall=new Game.Entity();wall.x=bounds.x()+bounds.width();wall.y=bounds.y();wall.width=1;wall.height=10;r.scene.objects.add(wall);
   r.update(.016,1,0);check(actor.animation.equals("idle"),"Do not run animation while blocked by wall");
  }
  try(var r=create(player,false)){
   r.update(.016,-1,0);r.setAction("shoot",true);r.update(.016,-1,0);var actor=r.scene.objects.get(0);
   check(actor.flipX&&actor.animation.equals("run_weapon"),"Gun follows left-facing player");
   check(r.scene.objects.stream().anyMatch(e->e.properties.getOrDefault("kind","").equals("bullet")&&e.properties.get("direction").equals("-1")),"Left-facing shot direction");
  }
  System.out.println("PASS: gun poses for keyboard/touch/gamepad, moving/jumping/falling, release, crouch walking");
 }
}
