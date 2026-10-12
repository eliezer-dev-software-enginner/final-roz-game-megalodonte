/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Collectible implements GameScript {
    public void onUpdate(double dt,GameContext c){var item=c.object();if(!item.visible||Actors.paused(c)||Actors.distance(c,item)>8)return;
        if(c.overlaps(item,Actors.player(c))){item.visible=false;item.solid=false;
            if(item.properties.get("kind").equals("coin")){int coins=Integer.parseInt(c.state().getOrDefault("run.coins","0"));c.state().put("run.coins",""+(coins+1));Actors.sound(c,"coinSound");}
            else{c.state().put("run.key."+item.properties.get("keyColor"),"true");Actors.sound(c,"keySound");}
        }
    }
}
