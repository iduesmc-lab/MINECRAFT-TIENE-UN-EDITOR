# Minecraft Tiene Un Editor 🎬

> «Instalé un mod que edita mis vídeos antes de que termine de jugarlos».

Mod de **Fabric para Minecraft 1.21.1**. Un director invisible convierte tu partida survival en un
vídeo absurdo. Si el público se aburre, **cambia la escena**.

## Cómo funciona

Arriba de la pantalla hay una barra: **🎬 Interés del público**. Baja sola con el tiempo, más rápido
si te quedas quieto o si te pasas el rato picando piedra. Sube cuando pasa algo interesante (te
hacen daño, matas monstruos, encuentras minerales). Cuando llega a cero, el director grita **¡CORTEN!**
y elige una escena.

Cada vez que mueres de verdad empieza una nueva **TOMA**.

### Escenas

| Cartel | Cuándo | Qué pasa |
|---|---|---|
| **CORTE. FALTA PRESUPUESTO** | Te aburres (sobre todo picando piedra) | Todo a tu alrededor se vuelve de cartón (solo en tu pantalla, el mundo no se toca). Un creeper de cartón, sostenido por un palo, corre hacia ti. Explota con un triste «pff». |
| **¡DOBLE DE ACCIÓN!** | Vas a morir | Un aldeano con tu cara y tu arma recibe el golpe mortal mientras tú sales disparado fuera de plano. Se recarga cada 4 minutos. |
| **EL PATROCINADOR EXIGE UNA MENCIÓN** | Picas un diamante | Recibes una *Pala Horrible™*. Tienes 40 s para escribir «Pala Horrible» en el chat. Mientras, los monstruos se quedan quietos y unos zombies figurantes esperan detrás de una cámara. Si cumples, el patrocinador paga (diamante extra + experiencia); si no, se lleva un diamante y los figurantes salen con ganas. |
| **GIRO DE GUION** | Te aburres | Aparece el villano principal, tu gemelo malvado (con tu cara) o empieza una escena de persecución. |
| **CAMBIO DE GÉNERO** | Te aburres, o con el megáfono | 45 s de terror, western, comedia romántica, musical, anime o documental de naturaleza, cada uno con sus efectos, sonidos y nombres para los monstruos. |

### El megáfono del director

A veces, en mitad de una escena, al director **se le cae el megáfono**. Tienes 20 segundos para
cogerlo. Con él en la mano:

- **Clic derecho**: dar la orden seleccionada.
- **Agachado + clic derecho**: cambiar de orden.

Órdenes:

- **◀◀ REPETICIÓN**: vuelves a donde estabas hace 5 segundos (y con la vida de entonces, si era más).
- **CÁMARA LENTA**: el servidor entero va a cámara lenta unos segundos.
- **CAMBIO DE GÉNERO**: tú eliges que esto ahora es un musical. Bueno, eliges que cambie; el género es aleatorio.

Pero el director **no se rinde**. Mientras tengas su megáfono, su *furia* (en la barra) sube, y cada
orden la sube más. Cuando se harta, contraataca:

- **¡DAME ESO!**: forcejea y el megáfono sale volando.
- **¡CORTEN!**: cancela tu género y te impone un recorte de presupuesto.
- **¡SEGURIDAD!**: manda a dos guardias del set. Si te golpean, te quitan el megáfono.

## Comandos (para probar)

Requieren permisos de operador (o trucos activados en un mundo de un jugador).

```
/director estado
/director escena presupuesto|patrocinador|villano|gemelo|persecucion
/director escena genero terror|western|romance|musical|anime|documental
/director megafono        # te da el megáfono
/director doble           # recarga el doble de acción
/director aburrimiento 100
/director corten          # termina todas las escenas
```

## Instalación

1. Instala [Fabric Loader](https://fabricmc.net/use/) para Minecraft **1.21.1**.
2. Pon en la carpeta `mods` el `.jar` de este mod y [Fabric API](https://modrinth.com/mod/fabric-api).

El mod funciona en servidor; los jugadores que también lo tengan instalado ven los carteles con
barras de cine, claqueta y «● REC». Sin el mod en el cliente se ven como títulos normales.

## Compilar

Necesitas **Java 25** para ejecutar Gradle (el mod se compila para Java 21).

```
./gradlew build
```

El `.jar` queda en `build/libs/`. GitHub Actions lo compila en cada push y lo sube como artefacto.
