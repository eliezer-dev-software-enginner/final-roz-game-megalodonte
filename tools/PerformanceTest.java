import megalodonte.engine.*;
import java.nio.file.*;
import java.util.*;
/** CPU update benchmark; not a device FPS benchmark. Works against old/new packaged jars. */
class PerformanceTest {
 public static void main(String[] args)throws Exception{
  var game=Codec.read(Files.readString(Path.of("game.json")));
  for(String id:List.of("level-1","level-9","level-16")){
   for(boolean combat:List.of(false,true)){
    game.entrySceneId=id;
    try(var r=new megalodonte.engine.Runtime(game,megalodonte.engine.Runtime.reflection(PerformanceTest.class.getClassLoader()),new megalodonte.engine.Runtime.Events(){})){
     var times=new double[360];
     for(int frame=0;frame<480;frame++){
      // Repeated shots/movement, with warm-up excluded from the report.
      r.setAction("shoot",combat&&frame%12==0);long start=System.nanoTime();r.update(1.0/60,combat?.5:0,0);
      if(frame>=120)times[frame-120]=(System.nanoTime()-start)/1e6;
     }
     Arrays.sort(times);System.out.printf(Locale.ROOT,"%s %s median=%.3fms p95=%.3fms max=%.3fms%n",id,combat?"combat":"idle",times[180],times[342],times[359]);
    }
   }
  }
 }
}
