import megalodonte.engine.*;
import java.nio.file.*;
import java.util.*;
/** Sweep every imported enemy; exercise each patrol with both 30 and 120 Hz host cadences. */
class PatrolTest {
 static String active="";
 static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
 static megalodonte.engine.Runtime.ScriptFactory factory=name->{
  if(!name.endsWith("Enemy"))return new GameScript(){};
  var actual=megalodonte.engine.Runtime.reflection(PatrolTest.class.getClassLoader()).create(name);
  return new GameScript(){public void onStart(GameContext c){actual.onStart(c);}public void onUpdate(double dt,GameContext c){if(c.object().id.equals(active))actual.onUpdate(dt,c);}};
 };
 public static void main(String[] args)throws Exception{
  var g=Codec.read(Files.readString(Path.of("game.json")));int checked=0,recovered=0;
  for(int phase=1;phase<=17;phase++){
   g.entrySceneId="level-"+phase;var source=g.scenes.stream().filter(s->s.id.equals(g.entrySceneId)).findFirst().orElseThrow();
   var original=new HashMap<String,double[]>();for(var e:source.objects)if(e.scriptClassName.endsWith("Enemy"))original.put(e.id,new double[]{e.x,e.y});
   try(var r=new megalodonte.engine.Runtime(g,factory,new megalodonte.engine.Runtime.Events(){})){
    var player=Hierarchy.find(r.scene,"player");player.properties.put("invincible","1000");int phaseCount=0;
    for(var enemy:r.scene.objects)if(original.containsKey(enemy.id)){
     double x=enemy.x,y=enemy.y;boolean flip=enemy.flipX;var authored=original.get(enemy.id);if(Math.hypot(x-authored[0],y-authored[1])>1e-4)recovered++;
     active=enemy.id;
     for(int hz:new int[]{30,120}){
      enemy.x=x;enemy.y=y;enemy.flipX=flip;int changes=0,streak=0,maxStreak=0;boolean previous=enemy.flipX;
      for(int frame=0;frame<hz*2;frame++){
       player.x=enemy.x+4;player.y=enemy.y;r.update(1./hz,0,0);
       if(enemy.flipX!=previous){changes++;streak++;maxStreak=Math.max(streak,maxStreak);}else streak=0;previous=enemy.flipX;
      }
      check(maxStreak<=1,"Consecutive-frame reversal: "+source.id+"/"+enemy.name+" @"+hz);
      check(changes<=12,"Excessive reversal rate: "+source.id+"/"+enemy.name+" @"+hz+" -> "+changes);
      check(Double.isFinite(enemy.x)&&Double.isFinite(enemy.y),"Invalid patrol position");
     }
     checked++;phaseCount++;
    }
    System.out.println("PASS "+source.id+": "+phaseCount+" enemy patrols at 30/120 Hz");
   }
  }
  double travel=-1;
  for(int hz:new int[]{30,120}){
   var flat=Game.demo();var scene=flat.scenes.get(0);scene.objects.clear();scene.worldWidth=100;scene.worldHeight=20;
   var player=new Game.Entity();player.id="player";player.x=15;player.y=1;player.trigger=true;player.properties.put("invincible","1000");scene.objects.add(player);
   var enemy=new Game.Entity();enemy.x=10;enemy.y=1;enemy.trigger=true;enemy.scriptClassName="game.finalroz.Enemy";enemy.properties.put("speed","1.5625");enemy.properties.put("hp","1");enemy.properties.put("flying","false");scene.objects.add(enemy);
   var floor=new Game.Entity();floor.x=0;floor.y=0;floor.width=100;floor.height=1;scene.objects.add(floor);active=enemy.id;
   try(var r=new megalodonte.engine.Runtime(flat,factory,new megalodonte.engine.Runtime.Events(){})){
    var actor=r.scene.objects.get(1);for(int frame=0;frame<hz*2;frame++)r.update(1./hz,0,0);
    double distance=actor.x-10;check(Math.abs(distance-3.125)<.01,"Walking speed must use elapsed seconds @"+hz);
    if(travel>=0)check(Math.abs(distance-travel)<.01,"Different movement speed at 30/120 Hz");travel=distance;
   }
  }
  System.out.println("PASS: "+checked+" enemy/NPC patrols; "+recovered+" embedded spawn poses recovered");
 }
}
