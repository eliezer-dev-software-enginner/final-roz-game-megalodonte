import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import megalodonte.engine.*;
import java.util.*;
import java.util.function.Function;
/** Pixel regression: a temporarily unavailable animation frame must not erase its actor. */
class VisualContinuityTest {
 public static void main(String[] args){Platform.startup(()->Platform.runLater(()->{try{
  Game game=Game.demo();var scene=game.scenes.get(0);scene.objects.clear();
  var actor=new Game.Entity();actor.x=2;actor.y=2;actor.width=2;actor.height=2;actor.assetPath="first";
  var animation=new Game.Animation();animation.frames=List.of("first","next");animation.fps=8;animation.loop=true;actor.animations.put("walk",animation);actor.animation="walk";scene.objects.add(actor);
  try(var runtime=new megalodonte.engine.Runtime(game)){
   var canvas=new Canvas(640,360);var ready=new WritableImage(8,8);for(int x=0;x<8;x++)for(int y=0;y<8;y++)ready.getPixelWriter().setColor(x,y,Color.BLUE);
   var draw=Class.forName("megalodonte.engine.editor.GamePainter").getDeclaredMethod("draw",Canvas.class,megalodonte.engine.Runtime.class,Function.class);draw.setAccessible(true);
   var view=CameraView.of(runtime.scene.camera,640,360);int x=(int)view.screenX(3),y=(int)view.screenY(3);
   var entity=runtime.scene.objects.get(0);
   for(int i=0;i<120;i++){
    entity.animationTime=i/8.;final boolean first=i==0;
    draw.invoke(null,canvas,runtime,(Function<String,Image>)path->first?ready:null);
    var pixel=canvas.snapshot(null,null).getPixelReader().getColor(x,y);
    if(pixel.getBlue()<.99||pixel.getRed()>.01||pixel.getGreen()>.01)throw new AssertionError("Actor blinked while loading frame "+i+": "+pixel);
   }
  }
  System.out.println("PASS: 120 unavailable-frame draws preserve the actor's last decoded pixels");Platform.exit();
 }catch(Throwable error){error.printStackTrace();System.exit(1);}}));}
}
