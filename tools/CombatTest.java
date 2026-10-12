import megalodonte.engine.*;
import java.util.*;
/** Impact must consume each bullet, including during the victim's visible death transition. */
class CombatTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static Game.Entity bullet(Game.Scene s){var b=new Game.Entity();b.x=4.7;b.y=5.5;b.width=b.height=.15;b.trigger=true;b.scriptClassName="game.finalroz.Projectile";b.properties.put("kind","bullet");b.properties.put("direction","1");s.objects.add(b);return b;}
 public static void main(String[] args){
  var g=Game.demo();var s=g.scenes.get(0);s.id="level-1";g.entrySceneId=s.id;s.worldWidth=30;s.worldHeight=30;s.objects.clear();
  var p=new Game.Entity();p.id="player";p.x=1;p.y=5;p.solid=false;s.objects.add(p);
  var enemy=new Game.Entity();enemy.x=5;enemy.y=5;enemy.height=2;enemy.trigger=true;enemy.scriptClassName="game.finalroz.Enemy";enemy.properties.put("kind","enemy");enemy.properties.put("hp","1");enemy.properties.put("speed","0");enemy.properties.put("flying","true");s.objects.add(enemy);
  try(var r=new megalodonte.engine.Runtime(g,megalodonte.engine.Runtime.reflection(CombatTest.class.getClassLoader()),new megalodonte.engine.Runtime.Events(){})){
   var victim=r.scene.objects.get(1);var first=bullet(r.scene);r.update(.02,0,0);r.update(.02,0,0);
   check(!r.scene.objects.contains(first),"First bullet disappears on impact");check(victim.properties.get("hp").equals("0"),"Enemy hit");
   var second=bullet(r.scene);r.update(.02,0,0);r.update(.02,0,0);
   check(!r.scene.objects.contains(second),"Second bullet cannot pass through the visible dying enemy");
   for(int i=0;i<35;i++)r.update(.02,0,0);check(victim.visible&&victim.opacity<1&&victim.rotation>0,"Death rotates and fades instead of freezing");
   for(int i=0;i<30;i++)r.update(.02,0,0);check(!victim.visible&&!victim.solid,"Death finishes and removes collision");
  }
  System.out.println("PASS: bullets consumed on live/dying enemy impact; progressive death transition");
 }
}
