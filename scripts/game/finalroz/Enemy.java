/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Enemy implements GameScript {
    private double direction=1,vy,originY,clock,deadTime;
    public void onStart(GameContext c){originY=c.object().y;}
    public void onUpdate(double dt,GameContext c){var e=c.object();if(!e.visible||Actors.paused(c)||Actors.distance(c,e)>45)return;
        if(Actors.number(e,"hp",1)<=0){
            e.solid=false;if(e.animations.containsKey("destroy")){if(deadTime==0)c.animate(e,"destroy");deadTime+=dt;var a=e.animations.get("destroy");if(deadTime>=a.frames.size()/a.fps)e.visible=false;}else e.visible=false;return;
        }
        clock+=dt;double speed=Actors.number(e,"speed",2);boolean fly=Boolean.parseBoolean(e.properties.getOrDefault("flying","false"));
        if(Actors.iso(c)){
            var p=Actors.player(c);double dx=p.x-e.x,dy=p.y-e.y,n=Math.hypot(dx,dy);if(n>0)c.move(e,dx/n*speed*dt,dy/n*speed*dt*.75);
        }else{
            if(!fly)vy=Math.max(-20,vy-32.8125*dt);
            else vy=(originY+.65*Math.sin(clock*2)-e.y)*4;
            if(!fly){double old=e.x;e.x+=direction*.6;boolean ahead=c.grounded(e);e.x=old;if(!ahead&&c.grounded(e))direction=-direction;}
            var m=c.move(e,direction*speed*dt,vy*dt);if(m.blockedX())direction=-direction;if(m.blockedY())vy=0;e.flipX=direction<0;
        }
        String animation=e.animations.containsKey("walk")?"walk":e.animations.containsKey("run")?"run":e.animation;if(!animation.isBlank())c.animate(e,animation);
        if(!Boolean.parseBoolean(e.properties.getOrDefault("harmless","false"))&&c.overlaps(e,Actors.player(c)))Actors.damage(c);
    }
}
