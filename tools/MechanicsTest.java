import megalodonte.engine.*;
import java.nio.file.*;
import java.util.*;
public class MechanicsTest {
 static final megalodonte.engine.Runtime.ScriptFactory FACTORY=megalodonte.engine.Runtime.reflection(MechanicsTest.class.getClassLoader());
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static void press(megalodonte.engine.Runtime r,String a){r.setAction(a,true);r.update(.025,0,0);r.setAction(a,false);}
 public static void main(String[] args)throws Exception{
  var game=Codec.read(Files.readString(Path.of("game.json")));
  for(int i=1;i<=17;i++){
   var g=Codec.copy(game);g.entrySceneId="level-"+i;
   try(var r=new megalodonte.engine.Runtime(g,name->name.endsWith("Player")||name.endsWith("Session")?FACTORY.create(name):new GameScript(){},new megalodonte.engine.Runtime.Events(){})){
    var p=Hierarchy.find(r.scene,"player");double start=p.y;for(int frame=0;frame<120;frame++)r.update(.025,0,0);
    if(i!=16)check(r.blocked(p,0,-.01),"Phase "+i+" spawn does not land on solid ground: "+start+" -> "+p.y);
    else {double x=p.x,y=p.y;for(int f=0;f<10;f++)r.update(.025,1,1);check(Math.hypot(p.x-x,p.y-y)>.1,"Isometric movement");}
    System.out.println("PASS spawn "+i);
   }
  }
  var g=Codec.copy(game);g.entrySceneId="level-1";
  try(var r=new megalodonte.engine.Runtime(g,FACTORY,new megalodonte.engine.Runtime.Events(){})){
   var p=Hierarchy.find(r.scene,"player");r.scene.objects.removeIf(e->e!=p&&!e.id.endsWith("-template"));r.scene.tileMaps.clear();
   p.x=20-p.colliderOffsetX;p.y=10-p.colliderOffsetY;var floor=new Game.Entity();floor.x=0;floor.y=9;floor.width=80;floor.height=1;floor.solid=true;r.scene.objects.add(floor);
   var enemy=new Game.Entity();enemy.x=20.8;enemy.y=10;enemy.width=.2;enemy.height=2;enemy.trigger=true;enemy.properties.put("kind","enemy");enemy.properties.put("hp","1");r.scene.objects.add(enemy);
   press(r,"shoot");for(int f=0;f<10;f++)r.update(.025,0,0);check(enemy.properties.get("hp").equals("0"),"Fast bullet hits narrow enemy without tunneling");
   var key=new Game.Entity();key.x=p.x+p.colliderOffsetX;key.y=p.y+p.colliderOffsetY;key.trigger=true;key.properties.put("kind","key");key.properties.put("keyColor","red");key.scriptClassName="game.finalroz.Collectible";r.scene.objects.add(key);
   var door=new Game.Entity();door.x=25;door.y=10;door.properties.put("keyColor","red");door.scriptClassName="game.finalroz.Door";r.scene.objects.add(door);r.update(.025,0,0);r.update(.025,0,0);check(!door.solid&&!door.visible,"Matching key opens door");
   var platform=new Game.Entity();platform.x=30;platform.y=10;platform.width=3;platform.height=.5;platform.properties.put("route","[[0,30,10],[2,34,10],[4,30,10]]");platform.scriptClassName="game.finalroz.Platform";r.scene.objects.add(platform);p.x=31-p.colliderOffsetX;p.y=10.5-p.colliderOffsetY;r.update(.025,0,0);double x=p.x;for(int f=0;f<20;f++)r.update(.025,0,0);check(p.x>x+.5,"Platform carries player");
  }
  g=Codec.copy(game);g.entrySceneId="level-16";
  try(var r=new megalodonte.engine.Runtime(g,FACTORY,new megalodonte.engine.Runtime.Events(){})){
   var p=Hierarchy.find(r.scene,"player");r.scene.objects.removeIf(e->e!=p&&!e.id.endsWith("-template"));press(r,"shoot");var bomb=r.scene.objects.stream().filter(e->e.properties.getOrDefault("kind","").equals("bomb")).findFirst().orElseThrow();
   var enemy=new Game.Entity();enemy.x=bomb.x+1;enemy.y=bomb.y;enemy.properties.put("kind","enemy");enemy.properties.put("hp","1");r.scene.objects.add(enemy);
   for(int f=0;f<110;f++)r.update(.025,0,0);check(enemy.properties.get("hp").equals("0"),"Bomb explosion damages enemy");for(int f=0;f<40;f++)r.update(.025,0,0);check(!r.scene.objects.contains(bomb),"Bomb lifecycle");
  }
  g=Codec.copy(game);g.entrySceneId="level-15";
  try(var r=new megalodonte.engine.Runtime(g,FACTORY,new megalodonte.engine.Runtime.Events(){})){
   var p=Hierarchy.find(r.scene,"player");r.scene.objects.removeIf(e->e!=p&&!e.id.endsWith("-template"));r.scene.tileMaps.clear();var water=new Game.Entity();water.x=10;water.y=5;water.width=30;water.height=20;water.trigger=true;water.properties.put("kind","water");r.scene.objects.add(water);p.x=15-p.colliderOffsetX;p.y=10-p.colliderOffsetY;double y=p.y;
   var pad=new GamepadInput();pad.axes(0,-1,0,0);for(int f=0;f<20;f++){pad.apply(r);r.update(.025,0,0);}check(p.properties.getOrDefault("swimming","false").equals("true")&&p.y>y+.5,"Swim upward using gamepad");
  }
  System.out.println("PASS: all spawn points, iso movement, bullets, keys/doors, moving platform and bomb");
 }
}
