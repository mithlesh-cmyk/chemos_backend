package chemos.chem_os.services;

public final class CompanySanitizer {

    private CompanySanitizer() {}

    public static String sanitizeDisplayName(String input){

        if(input==null){
            return null;
        }

        return input
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    public static String createSearchKey(String input){

        return input
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

}
