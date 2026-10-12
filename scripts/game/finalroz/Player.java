/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
import java.util.*;
public final class Player implements GameScript {
    private double vy,vx,spawnX,spawnY,coyote,jumpBuffer,shotTimer,standingHeight;private int airJumps=1;private boolean crouching;
    public void onStart(GameContext c){var p=c.object();spawnX=p.x;spawnY=p.y;standingHeight=p.colliderHeight;p.properties.put("invincible","0");}
    private void animation(GameContext c,String name){var p=c.object();if(p.animations.containsKey(name))c.animate(p,name);else if(p.animations.containsKey("idle"))c.animate(p,"idle");}
    private boolean jump(GameContext c){return c.actionPressed("space")||c.actionPressed("jump")||c.actionPressed("pad_a");}
    public void onUpdate(double dt,GameContext c){
        if(Actors.paused(c))return;var p=c.object();double invincible=Math.max(0,Actors.number(p,"invincible",0)-dt);p.properties.put("invincible",""+invincible);shotTimer=Math.max(0,shotTimer-dt);
        if(Actors.iso(c))isometric(c,dt);else if(!swimming(c,dt))platform(c,dt);
        boolean shoot=c.actionPressed("shoot")||c.actionPressed("x")||c.actionPressed("control")||c.actionPressed("ctrl")||c.actionPressed("pad_x");
        if(shoot&&shotTimer<=0){spawnWeapon(c);shotTimer=Actors.iso(c)?2.5:.2;}
        if(p.y<1){Actors.damage(c);p.x=spawnX;p.y=spawnY;vy=vx=0;airJumps=1;}
    }
    private boolean swimming(GameContext c,double dt){
        var p=c.object();boolean water=c.scene().objects.stream().anyMatch(e->e.properties.getOrDefault("kind","").equals("water")&&c.overlaps(p,e));
        if(!water){if(p.properties.remove("swimming")!=null){p.colliderHeight=standingHeight;crouching=false;}return false;}
        p.properties.put("swimming","true");p.colliderHeight=standingHeight*.55;crouching=false;
        double x=c.horizontal()+(c.actionDown("right")?1:0)-(c.actionDown("left")?1:0),y=c.vertical()+(c.actionDown("up")||c.actionDown("pad_up")||c.actionDown("pad_a")||c.actionDown("jump")||c.actionDown("space")?1:0)-(c.actionDown("down")||c.actionDown("pad_down")||c.actionDown("crouch")?1:0);
        vx=approach(vx,Math.max(-1,Math.min(1,x))*3.125,65.625*dt);vy=approach(vy,Math.max(-1,Math.min(1,y))*3.125,65.625*dt);c.move(p,vx*dt,vy*dt);Actors.face(p,vx);animation(c,"swim");return true;
    }
    private void platform(GameContext c,double dt){
        var p=c.object();boolean ground=c.grounded(p);coyote=ground?.12:Math.max(0,coyote-dt);if(ground)airJumps=1;
        boolean advanced=Boolean.parseBoolean(p.properties.getOrDefault("advanced","false"));
        boolean wantCrouch=advanced&&(c.actionDown("crouch")||c.actionDown("down")||c.vertical()<-.3||c.actionDown("pad_down"));
        if(wantCrouch&&!crouching){p.colliderHeight=standingHeight*.55;crouching=true;}
        if(!wantCrouch&&crouching){p.colliderHeight=standingHeight;var test=c.move(p,0,0);if(test.blockedY())p.colliderHeight=standingHeight*.55;else crouching=false;}
        double direction=Math.max(-1,Math.min(1,c.horizontal()+(c.actionDown("right")?1:0)-(c.actionDown("left")?1:0)));
        boolean run=c.actionDown("shift")||c.actionDown("run")||c.actionDown("pad_r1");double speed=advanced&&!run?5:10.9375;if(crouching)speed*=.45;
        vx=approach(vx,direction*speed,65.625*dt);Actors.face(p,vx);
        boolean jumping=jump(c);jumpBuffer=jumping?.15:Math.max(0,jumpBuffer-dt);
        if(jumpBuffer>0&&(coyote>0||airJumps>0)){if(coyote<=0)airJumps--;vy=15.625;jumpBuffer=0;coyote=0;ground=false;Actors.sound(c,"jumpSound");}
        vy=Math.max(-31.25,vy-32.8125*dt);double dx=vx*dt,dy=vy*dt,oldX=p.x,oldY=p.y;
        var movement=c.move(p,dx,dy);
        if(ground&&movement.blockedX()&&vy<=0){
            p.x=oldX;p.y=oldY;double step=Math.abs(dx)+.08;var up=c.move(p,0,step);var across=c.move(p,dx,0);
            if(up.blockedY()||across.blockedX()){p.x=oldX;p.y=oldY;movement=c.move(p,dx,dy);}
            else movement=c.move(p,0,-step-.12);
        }else if(ground&&vy<=0&&!movement.blockedY())movement=c.move(p,0,-Math.abs(dx)-.12);
        if(movement.blockedY()){if(vy<0)ground=true;vy=0;}
        String name=crouching?"crouch":!ground?(vy>0?"jumping":"falling"):Math.abs(vx)>.1?(advanced&&!run?"walk":"run"):"idle";animation(c,name);
    }
    private void isometric(GameContext c,double dt){
        var p=c.object();double x=c.horizontal()+(c.actionDown("right")?1:0)-(c.actionDown("left")?1:0),y=c.vertical()+((c.actionDown("up")||c.actionDown("pad_up"))?1:0)-((c.actionDown("down")||c.actionDown("pad_down"))?1:0);
        double length=Math.hypot(x,y);if(length>1){x/=length;y/=length;}c.move(p,x*9.6875*dt,y*7.265625*dt);
        if(length>.05){String direction;if(Math.abs(y)<Math.abs(x)*.414)direction=x>0?"side_right":"side_left";else if(Math.abs(x)<Math.abs(y)*.414)direction=y>0?"back":"front";else direction=(y>0?"45back_":"45front_")+(x>0?"right":"left");p.properties.put("facing",direction);animation(c,direction+"_walk");}
        else animation(c,p.properties.getOrDefault("facing","front")+"_idle");
    }
    private void spawnWeapon(GameContext c){
        var p=c.object();boolean bomb=Actors.iso(c);var template=c.findObject(bomb?"bomb-template":"bullet-template");var e=template.copy();e.id=UUID.randomUUID().toString();e.visible=true;e.solid=true;e.trigger=true;e.properties.put("kind",bomb?"bomb":"bullet");e.scriptClassName="game.finalroz.Projectile";
        var b=c.collisionBounds(p);double ew=e.colliderWidth>0?e.colliderWidth:e.width,eh=e.colliderHeight>0?e.colliderHeight:e.height;e.x=(bomb?b.x()+b.width()/2-ew/2:p.flipX?b.x()-ew-.05:b.x()+b.width()+.05)-e.colliderOffsetX;e.y=(bomb?b.y():b.y()+b.height()*.65-eh/2)-e.colliderOffsetY;
        e.properties.put("direction",p.flipX?"-1":"1");e.animationTime=0;if(bomb&&e.animations.containsKey("bomb"))c.animate(e,"bomb");c.scene().objects.add(e);if(!bomb)Actors.sound(c,"shootSound");
    }
    private static double approach(double current,double target,double amount){return current<target?Math.min(target,current+amount):Math.max(target,current-amount);}
}
