/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Projectile implements GameScript {
    private double age,vy;private boolean exploded;
    public void onUpdate(double dt,GameContext c){
        if(Actors.paused(c))return;var e=c.object();age+=dt;
        if(e.properties.get("kind").equals("bomb")){
            if(age>=2.46&&!exploded){exploded=true;Actors.sound(c,"bombSound");
                for(var target:c.scene().objects)if(target.solid&&target.properties.getOrDefault("kind","").equals("enemy")&&distance(c,target,e)<3)target.properties.put("hp","0");
                if(distance(c,Actors.player(c),e)<2.5)Actors.damage(c);
            }
            if(age>3.46)c.scene().objects.remove(e);return;
        }
        if(age>3){c.scene().objects.remove(e);return;}
        vy-=3.28125*dt;double dx=Actors.number(e,"direction",1)*25*dt,dy=vy*dt;int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(dx),Math.abs(dy))/.1));
        for(int step=0;step<steps;step++){
            var movement=c.move(e,dx/steps,dy/steps);
            if(movement.blockedX()||movement.blockedY()){c.scene().objects.remove(e);return;}
            for(var enemy:c.scene().objects)if(enemy.solid&&enemy.properties.getOrDefault("kind","").equals("enemy")&&c.overlaps(e,enemy)){
                enemy.properties.put("hp","0");enemy.solid=false;c.scene().objects.remove(e);return;
            }
        }
    }
    private double distance(GameContext c,Game.Entity a,Game.Entity b){var x=c.collisionBounds(a);var y=c.collisionBounds(b);return Math.hypot(x.x()+x.width()/2-y.x()-y.width()/2,x.y()+x.height()/2-y.y()-y.height()/2);}
}
