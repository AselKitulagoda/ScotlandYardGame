public class Chocolate extends Food {

  @Override
  //For a general animal eating chocolate
  public String eaten(Animal animal) {
    return "animal eats chocolate";
  }

  //For a dog eating food
  public String eaten(Dog dog) {
    return "dog eats chocolate";
  }

  //For a cat eating Chocolate
  public String eaten(Cat cat) {
    return "cat eats chocolate";
  }

}
