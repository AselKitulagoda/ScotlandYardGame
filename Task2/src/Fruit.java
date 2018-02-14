public class Fruit extends Food {

  @Override
  //For a general animal eating fruit
  public String eaten(Animal animal) {
    return "animal eats fruit";
  }

  //For a dog eating fruit
  public String eaten(Dog dog) {
    return "dog eats fruit";
  }

  //For a cat eating fruit
  public String eaten(Cat cat) {
    return "cat eats fruit";
  }

}
