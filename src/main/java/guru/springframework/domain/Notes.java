package guru.springframework.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(exclude = {"recipe"})
public class Notes {

    private String id;
    private Recipe recipe;
    private String recipeNotes;

}




