/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Platform implements GameScript {
    private double time;private double[][] route;
    public void onStart(GameContext c){route=Codec.GSON.fromJson(c.object().properties.getOrDefault("route","[]"),double[][].class);}
    public void onUpdate(double dt,GameContext c){if(Actors.paused(c)||route.length<2)return;var e=c.object();var p=Actors.player(c);var actor=c.collisionBounds(p);var top=c.collisionBounds(e);boolean riding=Math.abs(actor.y()-top.y()-top.height())<.05&&actor.x()<top.x()+top.width()&&actor.x()+actor.width()>top.x();
        double duration=route[route.length-1][0];if(duration<=0)return;time=(time+dt)%duration;int i=1;while(i<route.length-1&&route[i][0]<time)i++;double span=route[i][0]-route[i-1][0],t=span<=0?0:(time-route[i-1][0])/span;
        double x=route[i-1][1]+(route[i][1]-route[i-1][1])*t,y=route[i-1][2]+(route[i][2]-route[i-1][2])*t,dx=x-e.x,dy=y-e.y;
        e.x=x;e.y=y;if(riding){p.x+=dx;p.y+=dy;}
    }
}
