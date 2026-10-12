import megalodonte.engine.*;
import java.nio.file.*;
public class FacingTest {
 public static void main(String[] args)throws Exception{
  var face=Class.forName("game.finalroz.Actors").getDeclaredMethod("face",Game.Entity.class,double.class);face.setAccessible(true);
  var g=Codec.read(Files.readString(Path.of("game.json")));int count=0;
  for(var s:g.scenes)for(var e:s.objects)if(e.scriptClassName.equals("game.finalroz.Enemy")||e.scriptClassName.equals("game.finalroz.Player")){
   String nativeFacing=e.properties.get("spriteFacing");if(!java.util.Set.of("left","right").contains(nativeFacing))throw new AssertionError("Missing native orientation "+e.name);
   var clone=e.copy();face.invoke(null,clone,1.);if(clone.flipX!=nativeFacing.equals("left"))throw new AssertionError("Moving right: "+e.name);
   face.invoke(null,clone,-1.);if(clone.flipX!=nativeFacing.equals("right"))throw new AssertionError("Moving left: "+e.name);
   boolean held=clone.flipX;face.invoke(null,clone,0.);if(held!=clone.flipX)throw new AssertionError("Idle changes facing");count++;
  }
  System.out.println("PASS native-facing and motion direction for "+count+" actor instances");
 }
}
