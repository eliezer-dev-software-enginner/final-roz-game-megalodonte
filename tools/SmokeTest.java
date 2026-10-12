import megalodonte.engine.*;
import game.finalroz.Progress;
import java.nio.file.*;
import java.util.*;
public class SmokeTest {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static Game read()throws Exception{return Codec.read(Files.readString(Path.of("game.json")));}
 static void press(megalodonte.engine.Runtime r,String action){r.setAction(action,true);r.update(.025,0,0);r.setAction(action,false);}
 static class Events implements megalodonte.engine.Runtime.Events{
   Map<String,String> saved=Map.of();String exported,imported;
   public void saveState(Map<String,String> values){saved=Map.copyOf(values);}
   public Map<String,String> loadState(){return saved;}
   public void exportSave(String json,java.util.function.Consumer<String> done){exported=json;done.accept("OK");}
   public void importSave(java.util.function.BiConsumer<String,String> done){done.accept(imported,null);}
 }
 public static void main(String[] args)throws Exception{
  Game game=read();check(game.scenes.size()==48,"48 authored scenes");var factory=megalodonte.engine.Runtime.reflection(SmokeTest.class.getClassLoader());var events=new Events();
  try(var r=new megalodonte.engine.Runtime(game,factory,events)){
   press(r,"play");check(r.scene.id.equals("story-1"),"Play opens original story");press(r,"card:1");check(r.scene.id.equals("intro-1"),"Story opens LEVEL 1 card");press(r,"start:1");check(r.scene.id.equals("level-1"),"Start phase 1");
   var p=Hierarchy.find(r.scene,"player");for(int i=0;i<100;i++)r.update(.025,0,0);check(r.blocked(p,0,-.01),"Spawn lands on ground: "+p.y);
   double y=p.y;press(r,"space");for(int i=0;i<8;i++)r.update(.025,0,0);check(p.y>y+.5,"Jump");double air=p.y;press(r,"space");for(int i=0;i<8;i++)r.update(.025,0,0);check(p.y>air+.5,"Double jump");
   press(r,"shoot");check(r.scene.objects.stream().anyMatch(e->e.properties.getOrDefault("kind","").equals("bullet")),"Spawn bullet");for(int i=0;i<140;i++)r.update(.025,0,0);check(r.scene.objects.stream().noneMatch(e->e.properties.getOrDefault("kind","").equals("bullet")),"Bullet lifecycle");
   press(r,"menu");press(r,"options");press(r,"levels");press(r,"level:2");check(r.scene.id.equals("levels"),"Locked level cannot start");press(r,"options");
   r.state.put("progress.unlocked","4");r.state.put("best.level-1","1234");r.state.put("run.coins","12");press(r,"export");var portable=Progress.validate(SaveData.decode(game.applicationId,events.exported));check(!portable.containsKey("run.coins"),"No transient exported");check(portable.get("best.level-1").equals("1234"),"Record exported");
   events.imported=SaveData.encode(game.applicationId,Map.of("progress.unlocked","6","best.level-3","5000"));press(r,"import");check(r.state.get("progress.unlocked").equals("6"),"Import progress");var previous=Map.copyOf(r.state);events.imported=SaveData.encode(game.applicationId,Map.of("progress.unlocked","999"));press(r,"import");check(previous.equals(r.state),"Invalid import is atomic");
  }
  for(int level=1;level<=17;level++){
   Game g=Codec.copy(game);g.entrySceneId="level-"+level;
   // Exercise real collectible/session scripts; disable combat and movement only for exhaustive collection.
   var noCombat=(megalodonte.engine.Runtime.ScriptFactory)(name->name.endsWith("Session")||name.endsWith("Collectible")||name.endsWith("Door")?factory.create(name):new GameScript(){});
   try(var r=new megalodonte.engine.Runtime(g,noCombat,new Events())){
    var p=Hierarchy.find(r.scene,"player");var coins=r.scene.objects.stream().filter(e->e.properties.getOrDefault("kind","").equals("coin")).toList();check(!coins.isEmpty(),"Coins in phase "+level);
    for(var coin:coins){var b=megalodonte.engine.Runtime.collisionBounds(r.scene,coin);p.x=b.x()-p.colliderOffsetX;p.y=b.y()-p.colliderOffsetY;r.update(.025,0,0);}
    r.update(.025,0,0);check(r.scene.id.equals(level==17?"ending":Set.of(1,2,5,7,9,11,15,17).contains(level+1)?"story-"+(level+1):"intro-"+(level+1)),"Actual last coin completes phase "+level+": "+r.state.get("run.coins")+"/"+coins.size());
    check(r.state.containsKey("best.level-"+level),"Record phase "+level);System.out.println("PASS phase "+level+": "+coins.size()+" actual coins -> next scene");
   }
  }
  System.out.println("PASS: 17 phases, menus, ground, double jump, projectiles, locks, records and JSON roundtrip");
 }
}
