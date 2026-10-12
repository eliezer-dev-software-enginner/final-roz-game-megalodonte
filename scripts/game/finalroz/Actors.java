/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
import java.util.*;
final class Actors {
    private static final Map<Game.Scene,Game.Entity> PLAYERS=Collections.synchronizedMap(new WeakHashMap<>());
    static Game.Entity player(GameContext c){return PLAYERS.computeIfAbsent(c.scene(),s->c.findObject("player"));}
    static boolean paused(GameContext c){return Boolean.parseBoolean(c.state().getOrDefault("run.paused","false"));}
    static boolean iso(GameContext c){return c.scene().depthSort;}
    static double number(Game.Entity e,String key,double fallback){try{return Double.parseDouble(e.properties.getOrDefault(key,""+fallback));}catch(NumberFormatException ex){return fallback;}}
    static void sound(GameContext c,String key){if(c.state().getOrDefault("audio","true").equals("true")){
        String path=c.findObject("player").properties.get(key);if(path!=null&&!path.isBlank())c.playSound(path);
    }}
    static void damage(GameContext c){
        if(Actors.number(player(c),"invincible",0)>0)return;
        int lives=Integer.parseInt(c.state().getOrDefault("run.lives","25"))-1;c.state().put("run.lives",""+lives);
        player(c).properties.put("invincible","1.0");sound(c,"hurtSound");
        if(lives<=0){c.state().put("run.gameOver","true");c.pause(true);c.state().put("run.paused","true");}
    }
    static void face(Game.Entity e,double movement){if(Math.abs(movement)>1e-8)e.flipX=(movement<0)!=e.properties.getOrDefault("spriteFacing","right").equals("left");}
    static double distance(GameContext c,Game.Entity e){var p=player(c);return Math.hypot(e.x-p.x,e.y-p.y);}
}
