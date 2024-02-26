/* Helper class for a menu in the OnlineOrder app
   @author David Furcy
   @version CS 391 - Spring 2024 - A2
*/

class Menu
{
    private String[] options;  // menu option preceded by the menu's header

    Menu(String[] options)
    {
        this.options = options;
    }// constructor

    /* Return a string representation of a menu, starting with the header (at
       index 0), followed by the menu options, and then a prompt for an option
     */
    public String toString()
    {
        String result = options[0] + "\n";
        for(int i=1; i<options.length; i++) {
            result += "  " + i + ". " + options[i] + "\n";
        }
        result += "Choose an option between 1 and " + (options.length-1) + ": ";
        return result;
    }// toString method

    /* return the number of options in the menu (including the header as an
       option)
     */
    public int getNumOptions()
    {
        return options.length;
    }// getNumOptions method

    /* return the option at a given index
     */
    public String getOption(int index)
    {
        return options[index];
    }// getOption method

}// Menu class
