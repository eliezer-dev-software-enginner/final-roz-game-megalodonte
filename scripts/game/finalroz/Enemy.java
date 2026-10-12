/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Enemy implements GameScript {
    private double direction=1,vy,originY,clock,deadTime,deathRotation,turnWait;
    private boolean dying;
    private final Game.Entity floorProbe=new Game.Entity();
    public void onStart(GameContext c){
        var e=c.object();direction=e.flipX?-1:1;
        // Imported actors may partially overlap a floor; restore a free pose before patrol.
        c.resolvePenetration(e,2.5);originY=e.y;
        floorProbe.width=.08;floorProbe.height=.16;floorProbe.solid=false;
    }
    private boolean support(GameContext c,Game.Entity e,double direction){
        var bounds=c.collisionBounds(e);floorProbe.x=(direction>0?bounds.x()+bounds.width()+.08:bounds.x()-.08)-floorProbe.width/2;
        floorProbe.y=bounds.y()-.15;
        return c.blocked(floorProbe,0,0);
    }
    public void onUpdate(double dt,GameContext c){var e=c.object();if(!e.visible||Actors.paused(c)||dt<=0)return;
        if(Actors.number(e,"hp",1)<=0){
            e.solid=false;
            if(!dying){dying=true;deathRotation=e.rotation;if(e.animations.containsKey("destroy"))c.animate(e,"destroy");}
            deadTime+=dt;double duration=Actors.number(e,"deathSeconds",1.1);
            // Original AnimationPlayer rotates/fades the single death frame; it was imported as a frozen sprite.
            e.rotation=deathRotation+180*Math.min(1,deadTime/.8);
            e.opacity=Math.max(0,1-Math.max(0,deadTime-.5)/Math.max(.01,duration-.5));
            if(deadTime>=duration){e.opacity=0;e.visible=false;}return;
        }
        if(Actors.distance(c,e)>45)return;
        turnWait=Math.max(0,turnWait-dt);clock+=dt;double speed=Actors.number(e,"speed",2);boolean fly=Boolean.parseBoolean(e.properties.getOrDefault("flying","false"));
        if(Actors.iso(c)){
            var p=Actors.player(c);double dx=p.x-e.x,dy=p.y-e.y,n=Math.hypot(dx,dy);if(n>0)c.move(e,dx/n*speed*dt,dy/n*speed*dt*.75);
        }else{
            if(!fly)vy=Math.max(-20,vy-32.8125*dt);
            else vy=(originY+.65*Math.sin(clock*2)-e.y)*4;
            boolean turned=false;double dx=direction*speed*dt;
            if(!fly&&c.grounded(e)&&!support(c,e,direction)){
                if(turnWait==0&&support(c,e,-direction)){direction=-direction;turnWait=.2;turned=true;dx=direction*speed*dt;}
                else dx=0; // Both feet unsafe: hold facing instead of flickering each frame.
            }
            var m=c.move(e,dx,vy*dt);
            if(m.blockedX()&&!turned&&turnWait==0&&!c.blocked(e,-direction*Math.max(.01,speed*dt),0)){
                direction=-direction;turnWait=.2;
            }
            if(m.blockedY())vy=0;e.flipX=direction<0;
        }
        String animation=e.animations.containsKey("walk")?"walk":e.animations.containsKey("run")?"run":e.animation;if(!animation.isBlank())c.animate(e,animation);
        if(!Boolean.parseBoolean(e.properties.getOrDefault("harmless","false"))&&c.overlaps(e,Actors.player(c)))Actors.damage(c);
    }
}
