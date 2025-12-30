package automation.utils;

public class FuzzyMatch {

    public static int calculate(String s1, String s2) {
        String visited = (s2.length() < s1.length()) ? s2 : s1;
        String target = (s2.length() < s1.length()) ? s1 : s2;
        int[] costs = new int[target.length() + 1];

        for (int i = 0; i < costs.length; i++) {
            costs[i] = i;
        }

        for (int i = 1; i <= visited.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= target.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), visited.charAt(i - 1) == target.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[target.length()];
    }

    public static double ratio(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.isEmpty() && s2.isEmpty()) return 100.0;
        
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();
        
        int distance = calculate(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        return (1.0 - ((double) distance / maxLength)) * 100.0;
    }
}
