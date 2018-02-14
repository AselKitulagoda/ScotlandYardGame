public class Zoo {

  public static void main(String[] args) {
    System.out.println("Whoop! You have opened the zoo for business.");
  }

  public String feed(Animal animal, Food food) {
    return food.eaten(animal);
  }

  public String feed(Dog dog, Food food) {
    return food.eaten(dog);
  }
}
