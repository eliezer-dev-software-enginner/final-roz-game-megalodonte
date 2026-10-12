/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Menu implements GameScript {
    private int focus;private boolean focusVisible;
    public void onStart(GameContext c){c.pause(false);c.state().put("run.paused","false");c.state().putIfAbsent("progress.unlocked","1");c.state().putIfAbsent("audio","true");c.state().putIfAbsent("touch","true");refresh(c);}
    private void status(GameContext c,String text){c.findHud("message").text=text;}
    private void refresh(GameContext c){
        c.mute(!Boolean.parseBoolean(c.state().getOrDefault("audio","true")));
        for(var h:c.scene().hud){
            if(h.id.equals("audio"))h.text="ÁUDIO: "+(Boolean.parseBoolean(c.state().getOrDefault("audio","true"))?"ON":"OFF");
            if(h.id.equals("touch"))h.text="TOQUE: "+(Boolean.parseBoolean(c.state().getOrDefault("touch","true"))?"ON":"OFF");
            if(h.action.startsWith("level:")){int level=Integer.parseInt(h.action.substring(6));boolean enabled=level<=Progress.unlocked(c);h.setBooleanProperty("liberado",enabled);h.text=enabled?""+level:"🔒";h.color=enabled?"#ffffff":"#8993a5";}
            if(h.id.startsWith("record-")){String level=h.id.substring(7);h.text="FASE "+level+"    "+Progress.time(c.state().get("best.level-"+level));}
        }
    }
    private void action(GameContext c,String action){
        if(action.equals("audio")||action.equals("touch")){c.state().put(action,""+!Boolean.parseBoolean(c.state().getOrDefault(action,"true")));c.saveState();refresh(c);}
        else if(action.equals("play")){c.state().put("run.lives","25");c.changeScene("story-1");}
        else if(action.equals("options")||action.equals("help"))c.changeScene(action);
        else if(action.startsWith("card:"))c.changeScene("intro-"+action.substring(5));
        else if(action.equals("levels"))c.changeScene("levels");
        else if(action.equals("records"))c.changeScene("records");
        else if(action.equals("back"))c.changeScene("menu");
        else if(action.startsWith("level:")){int level=Integer.parseInt(action.substring(6));if(level<=Progress.unlocked(c)){c.state().put("run.lives","25");c.changeScene(entrance(level));}}
        else if(action.startsWith("start:"))c.changeScene("level-"+action.substring(6));
        else if(action.equals("export"))c.exportSave(Progress.portable(c.state()),text->status(c,text));
        else if(action.equals("import"))c.importSave(Progress::validate,text->{status(c,text);refresh(c);});
    }
    static String entrance(int level){return java.util.Set.of(1,2,5,7,9,11,15,17).contains(level)?"story-"+level:"intro-"+level;}
    public void onUpdate(double dt,GameContext c){
        var buttons=c.scene().hud.stream().filter(h->h.type.equals("button")&&HudPresentation.enabled(h)&&h.visible).toList();
        if(!buttons.isEmpty()){
            if(c.actionPressed("pad_left")||c.actionPressed("pad_right")||c.actionPressed("pad_up")||c.actionPressed("pad_down")||c.actionPressed("pad_a"))focusVisible=true;
            if(c.actionPressed("pad_right")||c.actionPressed("pad_down"))focus=(focus+1)%buttons.size();
            if(c.actionPressed("pad_left")||c.actionPressed("pad_up"))focus=(focus+buttons.size()-1)%buttons.size();focus%=buttons.size();
            for(var h:buttons)h.focused=focusVisible&&h==buttons.get(focus);
            if(c.actionPressed("pad_a")||c.actionPressed("enter")||c.actionPressed("space")||c.actionPressed("x")||c.actionPressed("pad_x"))action(c,buttons.get(focus).action);
        }
        if(c.actionPressed("pad_b")||c.actionPressed("escape"))action(c,"back");
        for(var h:c.scene().hud)if(!h.action.isBlank()&&c.actionPressed(h.action)&&HudPresentation.enabled(h))action(c,h.action);
    }
}
