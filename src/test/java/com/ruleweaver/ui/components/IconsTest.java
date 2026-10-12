package com.ruleweaver.ui.components;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IconsTest {

    @Test
    void everyApplicationIconResolvesFromMaterialDesign2Pack() {
        List<String> iconNames = List.of(
                Icons.DASHBOARD, Icons.APPLICANTS, Icons.POLICIES, Icons.EVALUATION,
                Icons.COMPARISON, Icons.PLUS, Icons.TRASH, Icons.EDIT, Icons.SEARCH,
                Icons.CLOSE, Icons.REFRESH, Icons.SWAP, Icons.EYE, Icons.PLAY,
                Icons.CHECK, Icons.SPARKLES, Icons.ARROW_UP, Icons.ARROW_DOWN,
                Icons.ARROW_RIGHT, Icons.INFO);

        for (String iconName : iconNames) {
            assertNotNull(Icons.create(iconName, 1, null).getIconCode(), iconName);
        }
    }

    @Test
    void plusUsesTheMaterialDesign2PackPrefix() {
        assertEquals("mdi2p-plus", Icons.create(Icons.PLUS, 1, null).getIconCode().getDescription());
    }

    @Test
    void unknownIconNamesFailWithAnActionableError() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Icons.create("not-a-ruleweaver-icon", 1, null));

        assertEquals("Unknown icon: not-a-ruleweaver-icon", error.getMessage());
    }
}
