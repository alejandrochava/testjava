package com.odontogram.bean;

import com.odontogram.entity.ToothNote;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@SessionScoped
public class OdontogramBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer selectedTooth;
    private String currentNote;

    // FDI notation: Upper Right (18-11), Upper Left (21-28), Lower Left (38-31), Lower Right (41-48)
    private final Integer[] upperRightTeeth = {18, 17, 16, 15, 14, 13, 12, 11};
    private final Integer[] upperLeftTeeth = {21, 22, 23, 24, 25, 26, 27, 28};
    private final Integer[] lowerLeftTeeth = {31, 32, 33, 34, 35, 36, 37, 38};
    private final Integer[] lowerRightTeeth = {48, 47, 46, 45, 44, 43, 42, 41};

    private EntityManagerFactory emf;

    @PostConstruct
    public void init() {
        emf = Persistence.createEntityManagerFactory("odontogramPU");
    }

    public void selectTooth(int toothNumber) {
        this.selectedTooth = toothNumber;
        loadNoteForTooth(toothNumber);
    }

    private void loadNoteForTooth(int toothNumber) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<ToothNote> query = em.createQuery(
                "SELECT t FROM ToothNote t WHERE t.toothNumber = :toothNumber ORDER BY t.updatedAt DESC",
                ToothNote.class
            );
            query.setParameter("toothNumber", toothNumber);
            query.setMaxResults(1);
            List<ToothNote> results = query.getResultList();
            if (!results.isEmpty()) {
                this.currentNote = results.get(0).getNote();
            } else {
                this.currentNote = "";
            }
        } finally {
            em.close();
        }
    }

    public void saveNote() {
        if (selectedTooth == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "Seleccione un diente primero", null));
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // Check if note already exists for this tooth
            TypedQuery<ToothNote> query = em.createQuery(
                "SELECT t FROM ToothNote t WHERE t.toothNumber = :toothNumber",
                ToothNote.class
            );
            query.setParameter("toothNumber", selectedTooth);
            query.setMaxResults(1);
            List<ToothNote> results = query.getResultList();

            ToothNote toothNote;
            if (!results.isEmpty()) {
                toothNote = results.get(0);
                toothNote.setNote(currentNote);
            } else {
                toothNote = new ToothNote(selectedTooth, currentNote);
                em.persist(toothNote);
            }

            em.getTransaction().commit();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Nota guardada para diente " + selectedTooth, null));
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al guardar: " + e.getMessage(), null));
        } finally {
            em.close();
        }
    }

    public boolean hasNote(int toothNumber) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(t) FROM ToothNote t WHERE t.toothNumber = :toothNumber AND t.note IS NOT NULL AND t.note <> ''",
                Long.class
            );
            query.setParameter("toothNumber", toothNumber);
            return query.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    public String getToothStyle(int toothNumber) {
        StringBuilder style = new StringBuilder();
        if (selectedTooth != null && selectedTooth == toothNumber) {
            style.append("selected-tooth ");
        }
        if (hasNote(toothNumber)) {
            style.append("has-note ");
        }
        return style.toString().trim();
    }

    public List<ToothNote> getAllNotes() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<ToothNote> query = em.createQuery(
                "SELECT t FROM ToothNote t WHERE t.note IS NOT NULL AND t.note <> '' ORDER BY t.toothNumber",
                ToothNote.class
            );
            return query.getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    public void deleteNote(ToothNote toothNote) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ToothNote managed = em.find(ToothNote.class, toothNote.getId());
            if (managed != null) {
                em.remove(managed);
            }
            em.getTransaction().commit();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Nota eliminada del diente " + toothNote.getToothNumber(), null));

            // Clear selection if deleted tooth was selected
            if (selectedTooth != null && selectedTooth.equals(toothNote.getToothNumber())) {
                selectedTooth = null;
                currentNote = "";
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error al eliminar: " + e.getMessage(), null));
        } finally {
            em.close();
        }
    }

    public void modifyNote(ToothNote toothNote) {
        selectTooth(toothNote.getToothNumber());
    }

    // Getters and setters
    public Integer getSelectedTooth() {
        return selectedTooth;
    }

    public void setSelectedTooth(Integer selectedTooth) {
        this.selectedTooth = selectedTooth;
    }

    public String getCurrentNote() {
        return currentNote;
    }

    public void setCurrentNote(String currentNote) {
        this.currentNote = currentNote;
    }

    public Integer[] getUpperRightTeeth() {
        return upperRightTeeth;
    }

    public Integer[] getUpperLeftTeeth() {
        return upperLeftTeeth;
    }

    public Integer[] getLowerLeftTeeth() {
        return lowerLeftTeeth;
    }

    public Integer[] getLowerRightTeeth() {
        return lowerRightTeeth;
    }
}
