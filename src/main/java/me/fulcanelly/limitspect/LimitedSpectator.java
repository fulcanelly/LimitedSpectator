package me.fulcanelly.limitspect;


import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.bukkit.event.block.Action.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;

class StatsSender {

    @SneakyThrows
    Socket getConnection() {
        return new Socket("localhost", 2022);
    }

    @SneakyThrows

    void send(String str) {
        var sock = getConnection();
        var o = new PrintStream(sock.getOutputStream());
        o.println(str);
    }

}

public class LimitedSpectator extends JavaPlugin implements Listener {
    
    StatsSender ss = new StatsSender();

    @EventHandler
    public void onMove(PlayerMoveEvent ev) {
        Location loc = ev.getTo();
        var was = loc.getDirection();
        var now = ev.getFrom().getDirection();

        var player =  ev.getPlayer();
//        player.spawnParticle(particle, location, count);
    player.spawnParticle(Particle.TOTEM, loc.clone(), 1, 0, 0, 0, 0);

        var entry = String.format(
                "{x: %f, y: %f, time: %d, name: %s }", 
                now.getX() - was.getX(), 
                now.getY() - was.getY(), 
                System.currentTimeMillis(),
                ev.getPlayer().getName()
            );
        System.out.println(entry);
        ss.send(entry); 
    }  

    public void onEnable() {

        var world = getServer().getWorlds().get(0);
        
        var finder = new AirPathFinder()
            .withFinish(new Location(world, 0, 5000, 0))
            .withStart(new Location(world, 1, 5003, 2)
        ).calckPath();

        // System.out.println(
        //     finder.getVisited()
        // );

        System.out.println(finder.getPath());

        System.out.println(
            finder.getVisited().size() + " - size"
        );
        

        ;
       // Bridge plugin = (Bridge)getServer().getPluginManager()
       getServer().getPluginManager().registerEvents(this, this);
       getServer().getPluginManager()
            .registerEvents(
                new ScenarioListenerGlue(), this
            );

       //getServer().getPluginCommand("d").register(commandMap)
    }
}

class ScenarioListenerGlue implements Listener {

    Map<String, UserScenario> scenarioByUsername = new HashMap<>();

    @EventHandler 
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        System.out.println("creating scenartion");
        var scenario = new UserScenario(player);
        System.out.println("starting scenartion");

        scenario.run();
        System.out.println("puting");

        scenarioByUsername.put(
            player.getName(), scenario
        );
        System.out.println("exit3");
        System.out.println("map:" + scenarioByUsername);

        
    }



    @EventHandler 
    public void onPlayerQuit(PlayerQuitEvent event) {
        var name = event.getPlayer().getName();
        var scenario = scenarioByUsername.remove(name);
        scenario.kill();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        scenarioByUsername.get(
            event.getPlayer().getName()
        ).getBreakingObserver().consume(event);
    }

    @EventHandler 
    public void onChatEvent(AsyncPlayerChatEvent event) {
        scenarioByUsername.get(
            event.getPlayer().getName()
        ).getExpectTextHidingAction().consume(event);
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        System.out.println("name:" + event.getPlayer().getName());
        System.out.println("map:" + scenarioByUsername);

        scenarioByUsername.get(
            event.getPlayer().getName()
        ).getRightClickObserver().consume(event);
    }

    
}

interface Waitable {
    

}
interface ScopedAction<T> {
    void setState(T t);
}

interface BlockBreakingWaiterHolder {

}


interface EventHolder {
    boolean isHaveToHandle();
}

interface WrappedAction<T> extends EventHolder {
    T get();
}

interface EventConsumer<T extends Event> {
    void consume(T event);
}
class Utils {
    @SneakyThrows
    static void waitFor(long time) {
        Thread.sleep(time);
    }

    //EventHolder/
    static <A, B> Either<A, B> race(WrappedAction<A> a, WrappedAction<B> b) {
        while (!a.isHaveToHandle() && !b.isHaveToHandle()) {
            waitFor(10l);
        }

        if (a.isHaveToHandle()) {
            return Either.makeLeft(a.get());
        }

        if (b.isHaveToHandle()) {
            return Either.makeRight(b.get());
        }

        throw new Error("What a... disaster");
    }

}

// R -- return type
// E -- event type 
abstract class Action<R, E extends Event> implements WrappedAction<R>, EventConsumer<E> {
    <A> Either<A, R> or(WrappedAction<A> a) {

        while (true) {
            if (a.isHaveToHandle() || this.isHaveToHandle()) {
                break;
            }
            Utils.waitFor(10l);
        }

        if (a.isHaveToHandle()) {
            return Either.makeLeft(a.get());
        }

        if (isHaveToHandle()) {
            return Either.makeRight(get());
        }

        throw new Error("What a... disaster");
    }
} 


@Data @With @AllArgsConstructor @RequiredArgsConstructor
class BlockBreakingObserver extends Action<Block, BlockBreakEvent> {

     //   return null;
    //} 
    
    BlockingQueue<Block> queue = new ArrayBlockingQueue<>(6);

    Optional<Block> targetBlock = Optional.empty();

    
    @Override
    public Block get() {
        return waitToBreak();
    }

    @Override
    public void consume(BlockBreakEvent event) {
        tryFeedBreakBlockEvent(
            event.getBlock().getLocation()
        );        
    }

    boolean tryFeedBreakBlockEvent(Location l) {
        if (targetBlock.isEmpty()) {
            return false;
        }

        System.out.println("user breaks block: " + l.toString());
        System.out.println("target block: " + targetBlock.get().getLocation());

        
        if (!l.equals(targetBlock.get().getLocation())) {
            return false;
        }

        return queue.add(l.getBlock());
    }
    
    @SneakyThrows
    Block waitToBreak() {
        var result = queue.take();
        this.setTargetBlock(
            Optional.empty()
        );
        return result;
    }

    public boolean isHaveToHandle() {
        return queue.size() > 0;
    }

   // @Override
    public <A> Either<A, Block> or(WrappedAction<A> a) {
        //toodo: replace observer in state 
        return super.or(a);
      
    }


}

class ScenarioManager {
    Scenario loadByUser(String text) {
        return null;
    }
}



@With @AllArgsConstructor @NoArgsConstructor @Data
class RightClickObserver extends Action<Boolean, PlayerInteractEvent> {
    BlockingQueue<Boolean> queue = new ArrayBlockingQueue<>(6);
    boolean expect = false;

    @Override @SneakyThrows
    public Boolean get() {
        queue.take();
        return true;
    }

    @Override
    public boolean isHaveToHandle() {
        return queue.size() > 0;
    }

    @Override
    public void consume(PlayerInteractEvent event) {
        var action = event.getAction();
        if (expect && action == RIGHT_CLICK_BLOCK) {
            queue.add(true);
        }
    }
    
}

class DelayedItem<T> {  
    BlockingQueue<T> queue = new ArrayBlockingQueue<>(6);
    
    @SneakyThrows
    T getItem() {
        return queue.take();
    }

    void putItem(T val) {
        queue.add(val);
    }

    boolean isEmpty() {
        return queue.isEmpty();
    }

    boolean isHaveValue() {
        return ! isEmpty();
    }


}

@With @AllArgsConstructor @NoArgsConstructor @Data
class ExpectTextHidingAction extends Action<String, AsyncPlayerChatEvent> {

    DelayedItem<String> item = new DelayedItem<>();
    boolean expects = false;

    @Override
    public String get() {
        return item.getItem();
    }

    @Override
    public boolean isHaveToHandle() {
        return item.isHaveValue();
    }

    @Override
    public synchronized void consume(AsyncPlayerChatEvent event) {
        if (expects) {
            item.putItem(event.getMessage());
            event.setCancelled(true);
            this.setExpects(false);
        }
        
    }
    
}
@Data
class MultiplexedBlockBreakingObserver {

}

interface Scenario {
    void run();
}


@Data @AllArgsConstructor
class Either<A, B> {
    A left;
    B right;
    
    static <A, B> Either<A, B> makeLeft(A left) {
        return new Either<>(left, null);
    }

    static <A, B> Either<A, B> makeRight(B right) {
        return new Either<>(null, right);
    }

    boolean isRight() {
        return right != null;
    }

    boolean isLeft() {
        return left != null;
    }
}

interface SwitchableScenario extends Scenario{
    
    void start();

    void setThread(Thread t);
    Thread getThread();

    default void run() {
        setThread(new Thread(this::start));
        getThread().start();
    }

    default void kill() {
        var thread = getThread();

        if (thread == null) { 
            return;
        }
        thread.stop();
        setThread(null);
    }

    default void switchScenario(SwitchableScenario scene) {
        scene.run();
        this.kill();
    }
}

@Data @RequiredArgsConstructor
abstract class BaseScenario implements SwitchableScenario {
    
    BlockBreakingObserver breakingObserver = new BlockBreakingObserver();
    RightClickObserver rightClickObserver = new RightClickObserver();
    ExpectTextHidingAction expectTextHidingAction = new ExpectTextHidingAction();
    
    final Player player;
    Thread thread;


    protected ExpectTextHidingAction setupExpectTextHidingAction() {
        return this.expectTextHidingAction = new ExpectTextHidingAction()
            .withExpects(true);
    }

    String expectText() {
        return setupExpectTextHidingAction().get();
    }

    protected BlockBreakingObserver setupBreakingObserver(Location target) {
        return this.breakingObserver = new BlockBreakingObserver()
            .withTargetBlock(
                Optional.of(target.getBlock())
            );
    }

    protected RightClickObserver setupClickObserver() {
        return this.rightClickObserver = new RightClickObserver()
            .withExpect(true);
    }


    synchronized BlockBreakingObserver waitToBreak(Location loc) {
        breakingObserver = setupBreakingObserver(loc);
        breakingObserver.waitToBreak();
        return breakingObserver;
    }


    void say(String text) {
        player.sendMessage(text);
    }


    void sayRed(String text) {
        player.sendMessage(
            ChatColor.RED + text
        );
    }

    void sayYellow(String text) {
        player.sendMessage(
            ChatColor.YELLOW + text
        );
    }

    Location getUserLocation() {
        return player.getLocation().clone();
    }

    boolean expectLeftClick() {
        rightClickObserver = new RightClickObserver();
        return rightClickObserver.get();
    }

}

class RecordPicture {

}

@Data @AllArgsConstructor @With @NoArgsConstructor
class LinkedLocation {

    Location current;
    LinkedLocation parent;
    int counter = 0;

}

@Data @With @AllArgsConstructor @NoArgsConstructor
class AirPathFinder {
    Location start, finish;
    int counter = 0;

    List<LinkedLocation> visited = new ArrayList<LinkedLocation>();

    List<Location> visitedAsLocations() {
        return visited.stream().map(v -> v.getCurrent()).toList();
    }

    Stream<Location> getNonVisitedEmptyNear(Location loc) {
        var near = Stream.of(
            loc.clone().add(-1, 0, 0),
            loc.clone().add(1, 0, 0),
            loc.clone().add(0, 1, 0),
            loc.clone().add(0, -1, 0),
            loc.clone().add(0, 0, 1),
            loc.clone().add(0, 0, -1)
        ).filter(
            l -> l.getBlock().isEmpty()
        ).filter(
            l -> ! visitedAsLocations().stream().anyMatch(i -> l.equals(i))
        ).toList();

        // System.out.println("visited: " + visitedAsLocations());
        // System.out.println("near List: " + near);
        return near.stream();
        
    }

    Stream<LinkedLocation> getAllFromLevel(final int level) {
        return visited.stream()
            .filter(l -> l.getCounter() == level);
    }

    int getMaxCounter(List<LinkedLocation> list) {
        return list.stream()
            .max((a, b) -> a.counter - b.counter)
            .get().getCounter();
    }

    List<Location> getPath() {
        var res = new ArrayList<Location>(
            List.of(start)
        );

        var linked = visited.stream()
            .filter(it -> it.getCurrent().equals(start))
            .findAny().get();

        while (linked.getParent() != null) {
            linked = linked.getParent();
            res.add(linked.getCurrent());            
        } 
        

        return res;
    } 
    AirPathFinder calckPath() {
        visited.add(
            new LinkedLocation()
                .withCurrent(finish)
                .withCounter(0)
        );

        var allFromCurrentLevel = getAllFromLevel(counter).toList();
        //check if end it 
        while (!visitedAsLocations().contains(start)) {

            allFromCurrentLevel.stream()
                .map(l -> 
                    getNonVisitedEmptyNear(
                        l.getCurrent()
                    ).map(
                        i -> new LinkedLocation()
                            .withCurrent(i)
                            .withParent(l)
                            .withCounter(counter + 1)
                    ).toList()
                )
                .forEach(list -> visited.addAll(list));;

            counter ++;

            if (counter > 50) {
                return this;
            }
            allFromCurrentLevel = getAllFromLevel(counter).toList();
        }
        
        return this;
    }
    
    // List<Location> getPath() {
        
    //     var queue = new ArrayDeque<LinkedLocation>();
    //     counter = 0;

    //     var current = new LinkedLocation()
    //         .withCurrent(finish.clone());
        
    //         queue.add(current);

    //     while (!current.getCurrent().equals(start)) {
    //         // find nearby
    //         var nearby = getNonVisitedEmptyNear(
    //             queue.stream().map(i -> i.getCurrent()).toList(),
    //             current.getCurrent()
    //         );

    //         // add path back from nearby
    //         nearby.stream()
    //             .map(it -> new LinkedLocation()
    //                 .withCounter(current.getCounter() + 1)
    //                 .withParent(current.getParent())
    //                 .withCurrent(it)
    //             ).forEach(queue::add);;

    //         //var 
    //         if (queue.stream().filter(l -> l.getCounter() == counter).toList().isEmpty()) {
    //         //if (count(where loc.counter == count).isEmpty) {
    //             counter ++;
    //         }

    //         //            
    //         //set current = random(where loc.counter == counter)
    //     }

    //     return null;

    // }
}

class UserScenario extends BaseScenario {


    public UserScenario(Player player) {
        super(player);
    }



    void showArrow(Location from, Location to) {

    }

    void waitGoTo(Location loc) {

    }

    Either<Boolean, Block> expectRightClickOrBreakBlock(Location loc) {
        return setupBreakingObserver(loc).or(
            setupClickObserver() 
        );
    }

   
    //todo centralize events

    public void start() {
        while (true) {
            sayRed("Hi there, please break block under you");
            var playerLoc = getUserLocation();

            var res = expectRightClickOrBreakBlock(
                playerLoc.subtract(0, 2, 0)
            );
            
            if (res.isLeft()) {
                sayYellow("Good job");
            }
            
            if (res.isRight()) {
                sayRed("Ok");
            }

            say("enter command");
            say("your input: " + expectText());
            
        }
    }

  

}
