
public class Conversao {

    public Query query;
    public double result;

    public String getQuery() {
        return "Moeda de origem: " + query.getFrom()
                + "\nMoeda a ser convertida: " + query.getTo()
                + "\nQuantia a ser convertida: "+ query.getAmount();
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }
}
