/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Session implements GameScript {
    private double seconds;private int total,level;private boolean completed;
    public void onStart(GameContext c){
        c.setSoundVolume(Double.parseDouble(c.state().getOrDefault("soundVolume","0.35")));
        level=Integer.parseInt(c.scene().id.substring(6));total=(int)c.scene().objects.stream().filter(e->e.properties.getOrDefault("kind","").equals("coin")).count();
        c.state().put("run.coins","0");c.state().put("run.total",""+total);c.state().put("run.level",""+level);c.state().put("run.paused","false");c.state().put("run.gameOver","false");c.state().putIfAbsent("run.lives","25");
        for(String color:new String[]{"red","green","blue"})c.state().remove("run.key."+color);
        c.pause(false);for(var h:c.scene().hud)if(java.util.Set.of("left","right","up","down","jump","shoot","crouch","run").contains(h.id))h.visible=Boolean.parseBoolean(c.state().getOrDefault("touch","true"))&&(!h.id.equals("run")||(level==15||level==17))&&(!h.id.equals("crouch")||level>=15);
    }
    private void overlay(GameContext c,boolean visible,String text){c.findHud("message").visible=visible;c.findHud("message").text=text;c.findHud("resume").visible=visible&&!c.state().getOrDefault("run.gameOver","false").equals("true");c.findHud("retry").visible=visible;}
    public void onUpdate(double dt,GameContext c){
        if(c.actionPressed("menu")||c.actionPressed("pad_b")){c.pause(false);c.changeScene("menu");return;}
        if(c.actionPressed("retry")){c.state().put("run.lives","25");c.pause(false);c.changeScene(c.scene().id);return;}
        boolean paused=Actors.paused(c);
        if(c.actionPressed("pause")||c.actionPressed("escape")||c.actionPressed("pad_start")||c.actionPressed("resume")){
            if(!c.state().getOrDefault("run.gameOver","false").equals("true")){paused=!paused;c.state().put("run.paused",""+paused);c.pause(paused);}
        }
        overlay(c,paused,c.state().getOrDefault("run.gameOver","false").equals("true")?"FIM DE JOGO":"PAUSADO");
        if(!paused)seconds+=dt;
        int collected=Integer.parseInt(c.state().getOrDefault("run.coins","0"));
        c.findHud("status").text=String.format(java.util.Locale.ROOT,"FASE %d    MOEDAS %d/%d    VIDAS %s    TEMPO %.1f",level,collected,total,c.state().getOrDefault("run.lives","25"),seconds);
        if(!completed&&total>0&&collected>=total){completed=true;Progress.complete(c,level,seconds);c.pause(false);c.changeScene(level==17?"ending":Menu.entrance(level+1));}
    }
}
