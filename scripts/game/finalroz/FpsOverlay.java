/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
/** Frame cadence measured by the host before the physics delta clamp, even while paused. */
public final class FpsOverlay implements GameScript {
    private int displayed=-1;
    public void onStart(GameContext c){update(c);}
    public void onUpdate(double dt,GameContext c){update(c);}
    private void update(GameContext c){
        c.hud().visible=Boolean.parseBoolean(c.state().getOrDefault("showFps","false"));
        int fps=(int)Math.round(c.framesPerSecond());if(fps!=displayed){displayed=fps;c.hud().text="FPS: "+(fps>0?""+fps:"—");}
    }
}
