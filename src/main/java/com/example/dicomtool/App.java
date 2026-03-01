package com.example.dicomtool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Aplicación de escritorio Swing para DicomTool.
 * Lanzar con: java -jar dicomtool-0.1.0-shaded.jar
 */
public class App extends JFrame {

    private final JTextField txtFile1  = new JTextField(40);
    private final JTextField txtFile2  = new JTextField(40);
    private final JTextField txtCsv    = new JTextField(40);
    private final JTextArea  txtResult = new JTextArea(8, 50);
    private final JButton    btnRun    = new JButton("Comparar");
    private final JButton    btnClear  = new JButton("Limpiar");

    public App() {
        super("DicomTool – Comparador de pixeles DICOM");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Formulario de entrada ──────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 4, 4, 4);
        GridBagConstraints fc = new GridBagConstraints();
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(4, 0, 4, 4);
        GridBagConstraints bc = new GridBagConstraints();
        bc.insets = new Insets(4, 0, 4, 4);

        addRow(form, 0, "Archivo DICOM 1:", txtFile1, browseDicom(txtFile1), lc, fc, bc);
        addRow(form, 1, "Archivo DICOM 2:", txtFile2, browseDicom(txtFile2), lc, fc, bc);
        addRow(form, 2, "CSV de salida (opcional):", txtCsv, browseCsv(), lc, fc, bc);

        // ── Botones de acción ──────────────────────────────────────────────
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(btnClear);
        actions.add(btnRun);

        btnRun.setBackground(new Color(0x4A90D9));
        btnRun.setForeground(Color.WHITE);
        btnRun.setFocusPainted(false);

        // ── Área de resultados ─────────────────────────────────────────────
        txtResult.setEditable(false);
        txtResult.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtResult.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(txtResult);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultado"));

        // ── Layout final ───────────────────────────────────────────────────
        root.add(form, BorderLayout.NORTH);
        root.add(actions, BorderLayout.CENTER);
        root.add(scroll, BorderLayout.SOUTH);
        setContentPane(root);

        // ── Lógica ─────────────────────────────────────────────────────────
        btnRun.addActionListener(e -> runComparison());
        btnClear.addActionListener(e -> {
            txtFile1.setText("");
            txtFile2.setText("");
            txtCsv.setText("");
            txtResult.setText("");
        });
    }

    private void addRow(JPanel p, int row, String label,
                        JTextField field, JButton browse,
                        GridBagConstraints lc, GridBagConstraints fc, GridBagConstraints bc) {
        lc.gridx = 0; lc.gridy = row;
        p.add(new JLabel(label), lc);
        fc.gridx = 1; fc.gridy = row;
        p.add(field, fc);
        bc.gridx = 2; bc.gridy = row;
        p.add(browse, bc);
    }

    private JButton browseDicom(JTextField target) {
        JButton btn = new JButton("Examinar…");
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Archivos DICOM (*.dcm)", "dcm"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                target.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    private JButton browseCsv() {
        JButton btn = new JButton("Examinar…");
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));
            fc.setDialogTitle("Seleccionar o crear CSV");
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File sel = fc.getSelectedFile();
                if (!sel.getName().toLowerCase().endsWith(".csv")) {
                    sel = new File(sel.getAbsolutePath() + ".csv");
                }
                txtCsv.setText(sel.getAbsolutePath());
            }
        });
        return btn;
    }

    private void runComparison() {
        String path1 = txtFile1.getText().trim();
        String path2 = txtFile2.getText().trim();
        String csvPath = txtCsv.getText().trim();

        if (path1.isEmpty() || path2.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debes seleccionar los dos archivos DICOM.",
                    "Campos requeridos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File f1 = new File(path1);
        File f2 = new File(path2);
        if (!f1.exists()) { showError("El archivo no existe:\n" + path1); return; }
        if (!f2.exists()) { showError("El archivo no existe:\n" + path2); return; }

        btnRun.setEnabled(false);
        txtResult.setText("Procesando…");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                Main m = new Main();
                m.file1 = f1;
                m.file2 = f2;
                m.csv = csvPath.isEmpty() ? null : new File(csvPath);
                double mean1 = m.loadMean(f1);
                double mean2 = m.loadMean(f2);
                double diff  = Math.abs(mean1 - mean2);
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Archivo 1: %s%n  Media = %.4f%n%n", f1.getName(), mean1));
                sb.append(String.format("Archivo 2: %s%n  Media = %.4f%n%n", f2.getName(), mean2));
                sb.append(String.format("Diferencia absoluta = %.4f%n", diff));
                if (m.csv != null) {
                    m.writeCsv(mean1, mean2, diff);
                    sb.append(String.format("%nGuardado en CSV: %s", m.csv.getAbsolutePath()));
                }
                return sb.toString();
            }

            @Override
            protected void done() {
                try {
                    txtResult.setText(get());
                } catch (Exception ex) {
                    showError("Error al procesar: " + ex.getCause().getMessage());
                    txtResult.setText("");
                } finally {
                    btnRun.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        // Permite también uso CLI si se pasan argumentos
        if (args.length > 0) {
            Main.main(args);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new App().setVisible(true);
        });
    }
}
