package org.openjfx.app.entities.base;

import java.util.List;

import org.openjfx.app.core.MoveStrategy;
import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;



public abstract class LivingEntity extends MovableEntity {
    //Atribute
    protected MoveStrategy moveStrategy;
    private double hunger;
    private double thirst;
    private double health;

    private double hungerRate;
    private double thirstRate;
    private boolean isAlive;
    private double radius;
    protected  List<Entity> neighbors;


    //Constructor
    public LivingEntity(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape);
        this.health = initialHealth;
        this.hungerRate = hungerRate;
        this.thirstRate = thirstRate;
        this.hunger = 0.0;
        this.thirst = 0.0;
        this.isAlive = true;
        //this.moveStrategy = new FleeStrategy(); // Dang nhe la lang thang nhung chua co class nen de tam flee
    }


    //Getter & Setter
    public double getHealth() { return health; }
    public double getHunger() { return hunger; }
    public double getThirst() { return thirst; }
    public boolean isAlive() { return isAlive; }
    public double getRadius() { return radius; }
    


    public void setHealth(double health) {
        // Máu [0:100]
        this.health = Math.max(0, Math.min(100, health));
        
        
        if (this.health <= 0 && this.isAlive == true) {
            onDeath();
            this.isAlive = false;
        }
    }

    public void setHunger(double hunger) {
        // Đói thuộc [0:100]
        this.hunger = Math.max(0, Math.min(100, hunger));
    }

    public void setThirst(double thirst) {
        // Thirst thuộc [0;100]
        this.thirst = Math.max(0, Math.min(100, thirst));
    }

    public void setRadius(double radius){
        this.radius = Math.max(0, Math.min(100, radius));
    }

    //Method
    @Override
    public void update(double dt, WorldMap world) {
        if (!isAlive) {
            return;
        }

        this.neighbors = world.getNeighbors(this, radius);

        if (this.moveStrategy != null) {
            this.moveStrategy.updateVelocity(this, neighbors, dt, world);
        }
    
        super.move(dt);
    
        setHunger(this.hunger + hungerRate * dt);
        setThirst(this.thirst + thirstRate * dt);



        //Đói + Khát quá thì bị mất máu
        if (hunger >= 100 || thirst >= 100) {
            setHealth(this.health - 5*dt);
        }


    }

    public boolean hasThreat(Entity owner, List<Entity> neighbors) {
        for (Entity neighbor : neighbors){
            if (RelationManager.isScaredOf(owner.getType(), neighbor.getType())){
                return true;
            }
        }
        return false;
        
    }

    public abstract void eat();
    public abstract void drink();



    public void onDeath(){
        System.out.println("Death");
    }





    
}
