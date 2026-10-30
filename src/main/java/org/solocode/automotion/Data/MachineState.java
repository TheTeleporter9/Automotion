package org.solocode.automotion.Data;

/**
 * Mutable runtime state of one placed machine. Never shared between machines.
 */
public final class MachineState {

    private Recipe recipe;
    private int progressTicks;
    private boolean dirty = true;

    public Recipe recipe() {
        return recipe;
    }

    public String recipeId() {
        return recipe == null ? null : recipe.id();
    }

    public int progressTicks() {
        return progressTicks;
    }

    public void select(Recipe recipe) {
        this.recipe = recipe;
        this.progressTicks = 0;
        this.dirty = true;
    }

    public void clearRecipe() {
        if (recipe != null) {
            this.recipe = null;
            this.progressTicks = 0;
            this.dirty = true;
        }
    }

    public void setProgress(int progressTicks) {
        int clamped = Math.max(0, progressTicks);
        if (this.progressTicks != clamped) {
            this.progressTicks = clamped;
            this.dirty = true;
        }
    }

    public void resetProgress() {
        setProgress(0);
    }

    /**
     * Restores persisted state without marking the machine as modified.
     */
    public void restore(Recipe recipe, int progressTicks) {
        this.recipe = recipe;
        this.progressTicks = Math.max(0, progressTicks);
        this.dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void markClean() {
        dirty = false;
    }
}
