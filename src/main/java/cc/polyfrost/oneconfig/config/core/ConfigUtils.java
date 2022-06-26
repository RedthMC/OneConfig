package cc.polyfrost.oneconfig.config.core;

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionCategory;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.OptionSubcategory;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class ConfigUtils {
    public static BasicOption getOption(Option option, Field field, Object instance) {
        switch (option.type()) {
            case SWITCH:
                return ConfigSwitch.create(field, instance);
            case CHECKBOX:
                return ConfigCheckbox.create(field, instance);
            case INFO:
                return ConfigInfo.create(field, instance);
            case HEADER:
                return ConfigHeader.create(field, instance);
            case COLOR:
                return ConfigColorElement.create(field, instance);
            case DROPDOWN:
                return ConfigDropdown.create(field, instance);
            case TEXT:
                return ConfigTextBox.create(field, instance);
            case BUTTON:
                return ConfigButton.create(field, instance);
            case SLIDER:
                return ConfigSlider.create(field, instance);
            case KEYBIND:
                return ConfigKeyBind.create(field, instance);
            case DUAL_OPTION:
                return ConfigDualOption.create(field, instance);
        }
        return null;
    }

    public static ArrayList<BasicOption> getClassOptions(Object object) {
        ArrayList<BasicOption> options = new ArrayList<>();
        ArrayList<Field> fields = new ArrayList<>(Arrays.asList(object.getClass().getDeclaredFields()));
        Class<?> parentClass = object.getClass();
        Class<?> clazz = object.getClass();
        while (true) {
            clazz = clazz.getSuperclass();
            if (clazz != null && clazz != parentClass) fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            else break;
            parentClass = clazz;
        }
        for (Field field : fields) {
            Exclude exclude = findAnnotation(field, Exclude.class);
            if (exclude != null && exclude.type() != Exclude.ExcludeType.CONFIG) continue;
            Option option = findAnnotation(field, Option.class);
            if (option == null) continue;
            options.add(getOption(option, field, object));
        }
        return options;
    }

    public static BasicOption addOptionToPage(OptionPage page, Option option, Field field, Object instance, @Nullable Migrator migrator) {
        BasicOption configOption = getOption(option, field, instance);
        if (configOption == null) return null;
        if (migrator != null) {
            Object value = migrator.getValue(field, configOption.name, configOption.category, configOption.subcategory);
            if (value != null) setField(field, value, instance);
        }
        getSubCategory(page, configOption.category, configOption.subcategory).options.add(configOption);
        return configOption;
    }

    public static OptionSubcategory getSubCategory(OptionPage page, String categoryName, String subcategoryName) {
        if (!page.categories.containsKey(categoryName)) page.categories.put(categoryName, new OptionCategory());
        OptionCategory category = page.categories.get(categoryName);
        if (category.subcategories.size() == 0 || !category.subcategories.get(category.subcategories.size() - 1).getName().equals(subcategoryName))
            category.subcategories.add(new OptionSubcategory(subcategoryName));
        return category.subcategories.get(category.subcategories.size() - 1);
    }

    public static <T extends Annotation> T findAnnotation(Field field, Class<T> annotationType) {
        if (field.isAnnotationPresent(annotationType)) return field.getAnnotation(annotationType);
        for (Annotation ann : field.getDeclaredAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(annotationType))
                return ann.annotationType().getAnnotation(annotationType);
        }
        return null;
    }

    public static Object getField(Field field, Object parent) {
        try {
            field.setAccessible(true);
            return field.get(parent);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void setField(Field field, Object value, Object parent) {
        try {
            field.setAccessible(true);
            field.set(parent, value);
        } catch (Exception ignored) {
        }
    }
}
