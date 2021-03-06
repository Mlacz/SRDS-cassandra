import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CheckBook extends CassandraTableModel{
    private static final String TABLE_NAME = "BookRequest";
    private UUID requestId;
    private int id_user;
    private long timestamp;
    private boolean returned;
    private int single_id_book;
    private int single_requestedBooks;


    public CheckBook(Session session, UUID requestId, int id_user) {
        super(session);
        this.requestId = requestId;
        this.id_user = id_user;
    }

    public CheckBook(Session session, UUID requestId, int id_book, int id_user, int requestedBooks, boolean returned, long timestamp) {
        super(session);
        this.requestId = requestId;
        this.single_id_book = id_book;
        this.id_user = id_user;
        this.single_requestedBooks = requestedBooks;
        this.returned = returned;
        this.timestamp = timestamp;
    }

    public boolean CheckApproved(List<Integer> id_book,List<String> books_name){

        for (int i=0;i<id_book.size();i++){

            if (!singleCheckApproved(id_book.get(i),books_name.get(i))){
                return false;
            }
        }
        return true;
    }

    private boolean singleCheckApproved(int id_book, String book_name){

        StringBuilder sb = new StringBuilder("SELECT total_books FROM allbooks WHERE id_book = ").append(id_book).append(" AND book_name='").append(book_name).append("'");

        String query = sb.toString();
        System.out.println(query);
        ResultSet rs = execute(query);
        int totalBooks ;

        totalBooks = rs.one().getInt("total_books");

        List<CheckBook> requestBooks;
        requestBooks = getRelevant(id_book);

        boolean partialDecision = false;
        for (CheckBook requestBook : requestBooks){
            if (totalBooks >= requestBook.single_requestedBooks) {
                totalBooks -= requestBook.single_requestedBooks;

                if (requestId.equals(requestBook.requestId)){
                        partialDecision = true;
                        break;
                }
            }
        }

        return partialDecision;

    }

    private List<CheckBook> getRelevant (int idBook){

        List<CheckBook> requestBooks = new ArrayList<>();

        StringBuilder sb = new StringBuilder("SELECT * FROM ").append(TABLE_NAME).append(" WHERE id_book = ").append(idBook);

        String query = sb.toString();
        ResultSet rs = execute(query);

        rs.forEach(r -> {
            if (!r.getBool("returned")){
                requestBooks.add(new CheckBook(
                        session,
                        r.getUUID("id"),
                        r.getInt("id_book"),
                        r.getInt("id_user"),
                        r.getInt("req_books"),
                        r.getBool("returned"),
                        r.getLong("timestamp")));
            }

        });

        requestBooks.sort(new Comparator<CheckBook>() {
            @Override
            public int compare(CheckBook m1, CheckBook m2) {
                if (m1.timestamp == m2.timestamp) {
                    return 0;
                }
                return m1.timestamp < m2.timestamp ? -1 : 1;
            }
        });
        return requestBooks;
    }

}
