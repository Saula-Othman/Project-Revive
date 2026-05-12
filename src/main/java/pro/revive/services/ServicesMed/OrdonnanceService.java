package pro.revive.services.ServicesMed;

import pro.revive.entities.EntitiesMed.Ordonnance;
import pro.revive.interfaces.IService;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceService implements IService<Ordonnance> {

    Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public void addEntity(Ordonnance o) {
        String sql = "INSERT INTO ordonnances (id_consultation, medicament, posologie, duree_jours) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, o.getIdConsultation());
            ps.setString(2, o.getMedicament());
            ps.setString(3, o.getPosologie());
            ps.setInt(4, o.getDureeJours());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) o.setIdOrdo(keys.getInt(1));
            System.out.println("Ordonnance ajoutee ! ID=" + o.getIdOrdo());
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void deleteEntity(Ordonnance o) {
        try {
            PreparedStatement ps = cnx.prepareStatement("DELETE FROM ordonnances WHERE id_ordo=?");
            ps.setInt(1, o.getIdOrdo()); ps.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void updateEntity(int id, Ordonnance o) {
        try {
            PreparedStatement ps = cnx.prepareStatement(
                "UPDATE ordonnances SET medicament=?, posologie=?, duree_jours=? WHERE id_ordo=?");
            ps.setString(1, o.getMedicament());
            ps.setString(2, o.getPosologie());
            ps.setInt(3, o.getDureeJours());
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public List<Ordonnance> getData() {
        List<Ordonnance> list = new ArrayList<>();
        try {
            ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM ordonnances ORDER BY id_ordo DESC");
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    public List<Ordonnance> getByConsultation(int idConsultation) {
        List<Ordonnance> list = new ArrayList<>();
        try {
            PreparedStatement ps = cnx.prepareStatement("SELECT * FROM ordonnances WHERE id_consultation=? ORDER BY id_ordo");
            ps.setInt(1, idConsultation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    public void deleteByConsultation(int idConsultation) {
        try {
            PreparedStatement ps = cnx.prepareStatement("DELETE FROM ordonnances WHERE id_consultation=?");
            ps.setInt(1, idConsultation); ps.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    private Ordonnance map(ResultSet rs) throws SQLException {
        Ordonnance o = new Ordonnance();
        o.setIdOrdo(rs.getInt("id_ordo"));
        o.setIdConsultation(rs.getInt("id_consultation"));
        o.setMedicament(rs.getString("medicament"));
        o.setPosologie(rs.getString("posologie"));
        o.setDureeJours(rs.getInt("duree_jours"));
        return o;
    }
}
