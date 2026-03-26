package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;

public abstract class LivingEntity extends MovableEntity {
    //Atribute
    private double hunger;
    private double thirst;
    private double health;
    

    private double hungerRate;
    private double thirstRate;
    private boolean isAlive;


    //Constructor
    public LivingEntity(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape);
        this.health = initialHealth;
        this.hungerRate = hungerRate;
        this.thirstRate = thirstRate;
        this.hunger = 0.0;
        this.thirst = 0.0;
        this.isAlive = true;
    }


    //Getter & Setter
    public double getHealth() { return health; }
    public double getHunger() { return hunger; }
    public double getThirst() { return thirst; }
    public boolean isAlive() { return isAlive; }


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


    //Method
    @Override
    public void update(double dt) {
        if (!isAlive) return;
    
        super.move(dt);
    
        setHunger(this.hunger + hungerRate * dt);
        setThirst(this.thirst + thirstRate * dt);

        //Nếu đói + khát thì phải đi kiếm ăn
        if (this.hunger > 70.0) {
            this.eat(); 
        }
        if (this.thirst > 70.0) {
            this.drink();
        }


        //Đói + Khát quá thì bị mất máu
        if (hunger >= 100 || thirst >= 100) {
            setHealth(this.health - 5*dt);
        }

    
        
    }
    //bat bc phai co trong lop con @Override
    

    public abstract void eat();
    public abstract void drink();

    public void onDeath(){
        System.out.println("Death");
    }





    
}
