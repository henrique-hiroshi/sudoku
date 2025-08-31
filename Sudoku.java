import java.util.*;

/**
 
 * Comandos no jogo:
 *  - set r c v   -> coloca o valor v (1..9) na linha r, coluna c (1..9)
 *  - clear r c   -> apaga a célula (se não for fixa)
 *  - hint        -> preenche uma dica correta
 *  - check       -> mostra se há erros e quantas células faltam
 *  - print       -> imprime o tabuleiro
 *  - solve       -> resolve e mostra a solução
 *  - restart     -> recomeça do início do mesmo desafio
 *  - help        -> mostra os comandos
 *  - quit        -> sai do jogo
 */
public class Sudoku {

    // ===================
    // ====== MAIN ======
    // ===================
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== Sudoku (console) ===");
        System.out.println("Escolha a dificuldade: easy | medium | hard");
        System.out.print("> ");
        String diff = sc.nextLine().trim().toLowerCase();
        if (!diff.equals("easy") && !diff.equals("medium") && !diff.equals("hard")) {
            System.out.println("(Dificuldade inválida, usando 'easy'.)");
            diff = "easy";
        }

        Game game = new Game(diff);
        game.print();
        help();

        while (true) {
            System.out.print("\n> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();
            try {
                switch (cmd) {
                    case "set":
                        if (parts.length != 4) {
                            System.out.println("Uso: set r c v");
                            break;
                        }
                        int r = Integer.parseInt(parts[1]);
                        int c = Integer.parseInt(parts[2]);
                        int v = Integer.parseInt(parts[3]);
                        if (game.set(r, c, v)) {
                            game.print();
                            if (game.isCompleteAndValid()) {
                                System.out.println("\n🎉 Parabéns! Você completou o Sudoku!");
                            }
                        }
                        break;
                    case "clear":
                        if (parts.length != 3) {
                            System.out.println("Uso: clear r c");
                            break;
                        }
                        r = Integer.parseInt(parts[1]);
                        c = Integer.parseInt(parts[2]);
                        game.clear(r, c);
                        game.print();
                        break;
                    case "hint":
                        if (game.hint()) {
                            game.print();
                            if (game.isCompleteAndValid()) {
                                System.out.println("\n🎉 Parabéns! Você completou o Sudoku!");
                            }
                        }
                        break;
                    case "check":
                        game.check();
                        break;
                    case "print":
                        game.print();
                        break;
                    case "solve":
                        game.solveAndShow();
                        break;
                    case "restart":
                        game.restart();
                        game.print();
                        break;
                    case "help":
                        help();
                        break;
                    case "quit":
                    case "exit":
                        System.out.println("Até mais!");
                        return;
                    default:
                        System.out.println("Comando desconhecido. Digite 'help' para ajuda.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Números inválidos.");
            }
        }
    }

    private static void help() {
        System.out.println("\nComandos:");
        System.out.println("  set r c v   -> coloca v na linha r, coluna c (1..9)");
        System.out.println("  clear r c   -> apaga célula");
        System.out.println("  hint        -> preenche uma dica correta");
        System.out.println("  check       -> verifica erros e progresso");
        System.out.println("  print       -> imprime o tabuleiro");
        System.out.println("  solve       -> mostra a solução completa");
        System.out.println("  restart     -> recomeça o mesmo desafio");
        System.out.println("  help        -> mostra esta ajuda");
        System.out.println("  quit        -> sair\n");
    }

   
    // ====== GAME =========
   
    static class Game {
        final String difficulty;
        final Board initial;
        Board current;
        final Board solution;

        Game(String difficulty) {
            this.difficulty = difficulty;
            Generator.Difficulty d = switch (difficulty) {
                case "hard" -> Generator.Difficulty.HARD;
                case "medium" -> Generator.Difficulty.MEDIUM;
                default -> Generator.Difficulty.EASY;
            };
            this.initial = Generator.generate(d);
            this.current = initial.copy();
            // resolve uma vez para manter solução guardada (rápido)
            Board sol = initial.copy();
            Solver.solve(sol);
            this.solution = sol;
        }

        void restart() {
            this.current = initial.copy();
            System.out.println("Jogo reiniciado.");
        }

        boolean set(int r, int c, int v) {
            if (!inRange(r) || !inRange(c) || v < 1 || v > 9) {
                System.out.println("Coordenadas/valor fora do intervalo (1..9).");
                return false;
            }
            if (initial.fixed[r-1][c-1]) {
                System.out.println("Célula fixa (não pode alterar).");
                return false;
            }
            if (!current.isValidPlacement(r-1, c-1, v)) {
                System.out.println("Jogada inválida: conflito na linha, coluna ou bloco.");
                return false;
            }
            current.cells[r-1][c-1] = v;
            return true;
        }

        void clear(int r, int c) {
            if (!inRange(r) || !inRange(c)) {
                System.out.println("Coordenadas fora do intervalo (1..9).");
                return;
            }
            if (initial.fixed[r-1][c-1]) {
                System.out.println("Célula fixa (não pode alterar).");
                return;
            }
            current.cells[r-1][c-1] = 0;
        }

        boolean hint() {
            // escolhe uma célula vazia e preenche com valor da solução
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (current.cells[r][c] == 0) {
                        current.cells[r][c] = solution.cells[r][c];
                        System.out.printf("Dica: (%d,%d) = %d\n", r+1, c+1, solution.cells[r][c]);
                        return true;
                    }
                }
            }
            System.out.println("Não há células vazias para dica.");
            return false;
        }

        void check() {
            int mistakes = 0;
            int empty = 0;
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    int cur = current.cells[r][c];
                    if (cur == 0) { empty++; continue; }
                    if (cur != solution.cells[r][c]) mistakes++;
                }
            }
            if (mistakes == 0 && empty == 0) {
                System.out.println("Perfeito! Tudo correto e completo.");
            } else {
                System.out.printf("Faltando: %d | Erros: %d\n", empty, mistakes);
            }
        }

        void print() { current.print(); }

        boolean isCompleteAndValid() {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (current.cells[r][c] != solution.cells[r][c]) return false;
                }
            }
            return true;
        }

        void solveAndShow() {
            Board copy = current.copy();
            if (Solver.solve(copy)) {
                copy.print();
            } else {
                System.out.println("Sem solução (isso não deveria ocorrer neste gerado).");
            }
        }

        private boolean inRange(int x) { return x >= 1 && x <= 9; }
    }


    // ===== BOARD =========

    static class Board {
        final int[][] cells = new int[9][9];
        final boolean[][] fixed = new boolean[9][9]; // true = pista fixa do puzzle

        Board() {}

        Board copy() {
            Board b = new Board();
            for (int r = 0; r < 9; r++) {
                System.arraycopy(this.cells[r], 0, b.cells[r], 0, 9);
                System.arraycopy(this.fixed[r], 0, b.fixed[r], 0, 9);
            }
            return b;
        }

        boolean isValidPlacement(int r, int c, int v) {
            if (v == 0) return true;
            // linha
            for (int j = 0; j < 9; j++) if (j != c && cells[r][j] == v) return false;
            // coluna
            for (int i = 0; i < 9; i++) if (i != r && cells[i][c] == v) return false;
            // bloco 3x3
            int br = (r/3)*3, bc = (c/3)*3;
            for (int i = br; i < br+3; i++)
                for (int j = bc; j < bc+3; j++)
                    if ((i != r || j != c) && cells[i][j] == v) return false;
            return true;
        }

        void print() {
            String h = "+=======+=======+=======+";
            for (int r = 0; r < 9; r++) {
                if (r % 3 == 0) System.out.println(h);
                for (int c = 0; c < 9; c++) {
                    if (c % 3 == 0) System.out.print("|");
                    int v = cells[r][c];
                    String s = (v == 0) ? "." : Integer.toString(v);
                    if (fixed[r][c]) {
                        // números fixos em negrito simulado com '*'
                        System.out.print(" " + s + "*");
                    } else {
                        System.out.print(" " + s + " ");
                    }
                }
                System.out.println("|");
            }
            System.out.println(h);
            System.out.println("(Números com * são pistas fixas)");
        }
    }

   
    // ===== SOLVER =========

    static class Solver {
        static boolean solve(Board b) {
            // Estratégia: escolhe a célula vazia com menos candidatos (MRV) e faz backtracking
            int[] pos = nextCellMRV(b);
            if (pos == null) return true; // completo
            int r = pos[0], c = pos[1];
            List<Integer> cand = candidates(b, r, c);
            for (int v : cand) {
                b.cells[r][c] = v;
                if (solve(b)) return true;
                b.cells[r][c] = 0;
            }
            return false;
        }

        static int countSolutions(Board b, int cap) {
            // conta soluções até o limite 'cap' (para checar unicidade)
            int[] pos = nextCellMRV(b);
            if (pos == null) return 1;
            int r = pos[0], c = pos[1];
            int count = 0;
            for (int v : candidates(b, r, c)) {
                b.cells[r][c] = v;
                count += countSolutions(b, cap - count);
                b.cells[r][c] = 0;
                if (count >= cap) return count;
            }
            return count;
        }

        private static int[] nextCellMRV(Board b) {
            int bestR = -1, bestC = -1, bestCount = 10;
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (b.cells[r][c] == 0) {
                        int cnt = candidates(b, r, c).size();
                        if (cnt < bestCount) {
                            bestCount = cnt;
                            bestR = r; bestC = c;
                            if (cnt == 1) return new int[]{r, c};
                        }
                    }
                }
            }
            return (bestR == -1) ? null : new int[]{bestR, bestC};
        }

        private static List<Integer> candidates(Board b, int r, int c) {
            boolean[] used = new boolean[10];
            for (int j = 0; j < 9; j++) used[b.cells[r][j]] = true; // linha
            for (int i = 0; i < 9; i++) used[b.cells[i][c]] = true; // coluna
            int br = (r/3)*3, bc = (c/3)*3;
            for (int i = br; i < br+3; i++)
                for (int j = bc; j < bc+3; j++)
                    used[b.cells[i][j]] = true;
            List<Integer> cand = new ArrayList<>();
            for (int v = 1; v <= 9; v++) if (!used[v]) cand.add(v);
            // ordem aleatória leve para diversificar
            Collections.shuffle(cand, RNG.rng);
            return cand;
        }
    }


    //  GENERATOR
    
    static class Generator {
        enum Difficulty { EASY, MEDIUM, HARD }

        static Board generate(Difficulty difficulty) {
            // 1) gera uma grade completa
            Board full = new Board();
            fillDiagonalBlocks(full); // ajuda o backtracking
            if (!fillRest(full, 0, 0)) throw new RuntimeException("Falha ao gerar grade completa");

            // 2) remove células garantindo solução única
            int holes = switch (difficulty) {
                case EASY -> 40;    // ~41 pistas
                case MEDIUM -> 50;  // ~31 pistas
                case HARD -> 58;    // ~23 pistas
            };
            Board puzzle = full.copy();
            // marca todas as células inicialmente como fixas
            for (int r = 0; r < 9; r++) Arrays.fill(puzzle.fixed[r], true);

            List<int[]> order = allPositionsShuffled();
            int removed = 0;
            for (int[] pos : order) {
                if (removed >= holes) break;
                int r = pos[0], c = pos[1];
                int backup = puzzle.cells[r][c];
                puzzle.cells[r][c] = 0;

                Board test = puzzle.copy();
                int solCount = Solver.countSolutions(test, 2); // 2 = checa se ficou >1
                if (solCount == 1) {
                    puzzle.fixed[r][c] = false; // agora é célula editável
                    removed++;
                } else {
                    puzzle.cells[r][c] = backup; // restaura
                }
            }

            // garante que pistas restantes estão marcadas como fixas=true
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (puzzle.cells[r][c] != 0) puzzle.fixed[r][c] = true;
                }
            }
            return puzzle;
        }

        private static void fillDiagonalBlocks(Board b) {
            for (int k = 0; k < 3; k++) {
                int br = k*3, bc = k*3;
                List<Integer> vals = new ArrayList<>();
                for (int v = 1; v <= 9; v++) vals.add(v);
                Collections.shuffle(vals, RNG.rng);
                int idx = 0;
                for (int r = br; r < br+3; r++)
                    for (int c = bc; c < bc+3; c++)
                        b.cells[r][c] = vals.get(idx++);
            }
        }

        private static boolean fillRest(Board b, int r, int c) {
            // encontra próxima célula vazia
            int[] pos = nextEmpty(b, r, c);
            if (pos == null) return true;
            r = pos[0]; c = pos[1];

            List<Integer> vals = new ArrayList<>();
            for (int v = 1; v <= 9; v++) vals.add(v);
            Collections.shuffle(vals, RNG.rng);
            for (int v : vals) {
                if (b.isValidPlacement(r, c, v) && b.cells[r][c] == 0) {
                    b.cells[r][c] = v;
                    if (fillRest(b, r, c)) return true;
                    b.cells[r][c] = 0;
                }
            }
            return false;
        }

        private static int[] nextEmpty(Board b, int sr, int sc) {
            for (int r = sr; r < 9; r++) {
                for (int c = (r == sr ? sc : 0); c < 9; c++) {
                    if (b.cells[r][c] == 0) return new int[]{r, c};
                }
            }
            for (int r = 0; r < 9; r++)
                for (int c = 0; c < 9; c++)
                    if (b.cells[r][c] == 0) return new int[]{r, c};
            return null;
        }

        private static List<int[]> allPositionsShuffled() {
            List<int[]> pos = new ArrayList<>();
            for (int r = 0; r < 9; r++)
                for (int c = 0; c < 9; c++)
                    pos.add(new int[]{r, c});
            Collections.shuffle(pos, RNG.rng);
            return pos;
        }
    }

    // RNG centralizado para reprodutibilidade opcional
    static class RNG {
        static final Random rng = new Random(); // se quiser fixar: new Random(42);
    }
}
