/**
 * A singleton that holds all configurations of the server.
 */
class Configurations {

    /** Singleton instance */
    private Configurations configurations;
    private Configurations() { }

    /** Returns the instance of the class */
    public Configurations getInstance() {
        if (this.configurations == null) {
            this.configurations = new Configurations();
        }
        return this.configurations;
    }

    // TODO adds configurations here
}
