# DicomTool

Herramienta de escritorio en Java para comparar dos archivos DICOM calculando la media de sus píxeles. Incluye interfaz gráfica (GUI) y modo de línea de comandos (CLI).

---

## Índice

1. [Requisitos](#requisitos)
2. [Estructura del proyecto](#estructura-del-proyecto)
3. [Construcción](#construcción)
4. [Uso — Interfaz gráfica](#uso--interfaz-gráfica)
5. [Uso — Línea de comandos](#uso--línea-de-comandos)
6. [Formato del CSV](#formato-del-csv)
7. [Pruebas](#pruebas)

---

## Requisitos

| Herramienta | Versión mínima |
| ----------- | -------------- |
| Java (JDK)  | 11             |
| Maven       | 3.6            |

---

## Estructura del proyecto

```
DicomTool/
├── pom.xml
└── src/
    ├── main/java/com/example/dicomtool/
    │   ├── App.java      ← Ventana Swing (punto de entrada por defecto)
    │   └── Main.java     ← Lógica DICOM + CLI (picocli)
    └── test/java/com/example/dicomtool/
        └── MainTest.java ← Tests JUnit 5
```

---

## Construcción

```bash
mvn package -DskipTests
```

El ejecutable se genera en `target/dicomtool-0.1.0.jar`.  
Incluye todas las dependencias (fat JAR), por lo que no requiere instalación adicional.

---

## Uso — Interfaz gráfica

Ejecutar el JAR sin argumentos abre la ventana de escritorio:

```bash
java -jar target/dicomtool-0.1.0.jar
```

### Pasos

1. **Archivo DICOM 1** → pulsa _Examinar…_ y selecciona el primer `.dcm`.
2. **Archivo DICOM 2** → pulsa _Examinar…_ y selecciona el segundo `.dcm`.
3. **CSV de salida** _(opcional)_ → pulsa _Examinar…_ para elegir un fichero nuevo o existente.
   - Si el fichero ya existe, las nuevas filas se **añaden al final** sin sobreescribir.
   - Si no existe, se crea con cabecera `file,mean` y una fila de diferencia.
4. Pulsa **Comparar**. Los resultados aparecen en el área inferior:

```
Archivo 1: paciente_a.dcm
  Media = 1024.3500

Archivo 2: paciente_b.dcm
  Media = 987.1200

Diferencia absoluta = 37.2300

Guardado en CSV: /home/usuario/resultados.csv
```

5. Pulsa **Limpiar** para resetear todos los campos y el área de resultados.

> **Nota:** el botón _Comparar_ se desactiva mientras se procesan los archivos para evitar ejecuciones dobles.

---

## Uso — Línea de comandos

El mismo JAR acepta argumentos posicionales cuando se lanza desde la terminal:

```bash
# Mostrar resultados en consola
java -jar target/dicomtool-0.1.0.jar <archivo1.dcm> <archivo2.dcm>

# Guardar en CSV (se añade si ya existe)
java -jar target/dicomtool-0.1.0.jar <archivo1.dcm> <archivo2.dcm> --csv resultados.csv

# Ayuda
java -jar target/dicomtool-0.1.0.jar --help
```

### Opciones

| Argumento / Opción | Requerido | Descripción            |
| ------------------ | --------- | ---------------------- |
| `archivo1.dcm`     | Sí        | Primer archivo DICOM   |
| `archivo2.dcm`     | Sí        | Segundo archivo DICOM  |
| `-c`, `--csv`      | No        | Ruta del CSV de salida |
| `-h`, `--help`     | No        | Muestra la ayuda       |

---

## Formato del CSV

El fichero CSV tiene dos columnas: `file` (ruta del archivo) y `mean` (media de píxeles).

**Primera vez que se crea:**

```csv
file,mean
/ruta/paciente_a.dcm,1024.3500
/ruta/paciente_b.dcm,987.1200
difference,37.2300
```

**Ejecuciones posteriores sobre el mismo CSV** (modo append):

```csv
file,mean
/ruta/paciente_a.dcm,1024.3500
/ruta/paciente_b.dcm,987.1200
difference,37.2300
/ruta/paciente_c.dcm,1100.0000
/ruta/paciente_d.dcm,1050.5000
```

---

## Pruebas

```bash
mvn test
```

Los tests cubren:

- Cálculo correcto de la media de píxeles (`testLoadMean`).
- Creación del CSV con cabecera y filas correctas (`testCsvOutput`).
- Comportamiento de append sobre un CSV existente (`testCsvAppend`).
