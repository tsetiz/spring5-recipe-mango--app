package guru.springframework.services;

import guru.springframework.commands.IngredientCommand;
import guru.springframework.converters.IngredientCommandToIngredient;
import guru.springframework.converters.IngredientToIngredientCommand;
import guru.springframework.domain.Ingredient;
import guru.springframework.domain.Recipe;
import guru.springframework.repositories.RecipeRepository;
import guru.springframework.repositories.UnitOfMeasureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class IngredientServiceImpl implements IngredientService {

    private final RecipeRepository recipeRepository;
    private final IngredientToIngredientCommand ingredientToIngredientCommand;
    private final IngredientCommandToIngredient ingredientCommandToIngredient;
    private final UnitOfMeasureRepository unitOfMeasureRepository;

    public IngredientServiceImpl(RecipeRepository recipeRepository, IngredientToIngredientCommand ingredientToIngredientCommand, IngredientCommandToIngredient ingredientCommandToIngredient, UnitOfMeasureRepository unitOfMeasureRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientToIngredientCommand = ingredientToIngredientCommand;
        this.ingredientCommandToIngredient = ingredientCommandToIngredient;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
    }

    @Override
    public IngredientCommand findByRecipeIdAndIngredientId(String recipeId, String ingredientId) {

        // it would be simpler in case we just search an ingredient by id with the help of IngredientRepository
        // using findByRecipeIdAndId method.

        Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeId);

        if (!optionalRecipe.isPresent()) {
            //todo implerror handling
            log.error("recipe Id not found. Id: " + recipeId);
        }

        Recipe recipe = optionalRecipe.get();

        Optional<IngredientCommand> optionalIngredientCommand = recipe.getIngredients().stream()
                .filter(ingredient -> ingredient.getId().equals(ingredientId))
                .map(ingredient -> ingredientToIngredientCommand.convert(ingredient)).findFirst();

        if (!optionalIngredientCommand.isPresent()) {
            //todo implerror handling
            log.error("Ingredient Id not found. Id: " + ingredientId);
        }

        return optionalIngredientCommand.get();
    }

    @Override
    @Transactional
    public IngredientCommand saveIngredientCommand(IngredientCommand command) {
        Optional<Recipe> recipeOptional = recipeRepository.findById(command.getRecipeId());
        if (!recipeOptional.isPresent()) {
            //todo impl error handling
            log.error("recipe id not found. Id: " + command.getId());
        }
        Recipe recipe = recipeOptional.get();
        Optional<Ingredient> optionalIngredient = recipe.getIngredients()
                .stream()
                .filter(ingredient -> ingredient.getId().equals(command.getId()))
                .findFirst();
        if (optionalIngredient.isPresent()) {
            Ingredient ingredientFound = optionalIngredient.get();
            ingredientFound.setDescription(command.getDescription());
            ingredientFound.setAmount(command.getAmount());
            ingredientFound.setUom(unitOfMeasureRepository.findById(command.getUom().getId())
                    .orElseThrow(() -> new RuntimeException("uom not found"))); //todo address this
        } else {
            //add new Ingredient
            Ingredient ingredient = ingredientCommandToIngredient.convert(command);
            //  ingredient.setRecipe(recipe); already done inside addIngredient (for two way directional flow)
            recipe.addIngredient(ingredient);
        }
        Recipe savedRecipe = recipeRepository.save(recipe);

        Optional<Ingredient> savedIngredientOptional = savedRecipe.getIngredients().stream()
                .filter(recipeIngredients -> recipeIngredients.getId().equals(command.getId()))
                .findFirst();

        //check by description
        if (!savedIngredientOptional.isPresent()) {
            //not totally safe... But best guess
            savedIngredientOptional = savedRecipe.getIngredients().stream()
                    .filter(recipeIngredients -> recipeIngredients.getDescription().equals(command.getDescription()))
                    .filter(recipeIngredients -> recipeIngredients.getAmount().equals(command.getAmount()))
                    .filter(recipeIngredients -> recipeIngredients.getUom().getId().equals(command.getUom().getId()))
                    .findFirst();
        }

        //to do check for fail
        return ingredientToIngredientCommand.convert(savedIngredientOptional.get());
    }

    @Override
    public void deleteById(String recipeId, String id) {

        //todo

        //deleting an Ingredient with this implementation removes the ingredient from the recipe,
        // but it does not delete the ingredient itself from the DB. In other words, after deleting an ingredient from a recipe,
        // the ingredient remains dangling in the DB, not related to any recipe. (Use ingredient repository instead or use orphanRemoval = true)


        //@OneToMany(cascade = CascadeType.ALL, mappedBy = "recipe", orphanRemoval = true)
        //private Set<Ingredient> ingredients = new HashSet<>();
        // if we add orphanRemoval = true  into the ingredients  set of Recipe  object then update the Recipe via recipeForm ,
        // all the ingredients  records that associated with the Recipe  are removed in Database.It happens because the form
        // sends an empty set of ingredients  to server and when the ingredients  is empty, hibernate removes all records in database that
        // associated with the Recipe. To solve this problem in recipeForm.html we need to bind the ingredient object properly.

        Optional<Recipe> optionalRecipe = recipeRepository.findById(recipeId);
        if (optionalRecipe.isPresent()) {
            log.debug("recipe found.Id: " + recipeId);
            Recipe recipe = optionalRecipe.get();
            Optional<Ingredient> optionalIngredient = recipe.getIngredients()
                    .stream()
                    .filter(ingredient -> ingredient.getId().equals(id))
                    .findFirst();
            if (optionalIngredient.isPresent()) {
                Ingredient ingredient = optionalIngredient.get();
                ingredient.setRecipe(null);
                recipe.getIngredients().remove(ingredient);
                recipeRepository.save(recipe);
            } else {
                log.error("ingredient not found.Id: " + id);
            }
        } else {
            log.error("recipe id not found.Id: " + recipeId);
        }
    }
}
