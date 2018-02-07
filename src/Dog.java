public class Dog extends Animal {

  public String eat(Food food) {
    return food.eaten(this);
  }

}
