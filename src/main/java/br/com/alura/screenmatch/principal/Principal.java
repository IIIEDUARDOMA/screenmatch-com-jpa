package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=f1cb3336";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series;
    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while(opcao != 0){
            var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar séries buscadas
                4 - Buscar séries por título
                5 - Buscar séries por ator
                6 - Listar top 5 séries
                7 - Buscar séries por categoria/gênero
                8 - Listar séries por quantidade de temporadas
                9 - Buscar episódios por trecho do título
                10 - Top 5 episódios por série
                11 - Buscar episódios a partir do ano
                
                0 - Sair                                 
                """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    listarTop5series();
                    break;
                case 7:
                    buscarSeriePorCategoria();
                    break;
                case 8:
                    buscarSeriesPorTamanho();
                    break;
                case 9:
                    buscarEpisodiosPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosPorAno();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie series = new Serie(dados);
        //dadosSeries.add(dados);
        repositorio.save(series);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Digite o nome da serie:");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serie.isPresent()){
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(),e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }else {
            System.out.println("Serie nao encontrada!");
        }
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }
    private void buscarSeriePorTitulo() {
        System.out.println("Digite o nome da serie:");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()){
            System.out.println("Dados da série: " + serieBusca);
        }else {
            System.out.println("Série não encontrada!");
        }
    }

    private  void buscarSeriePorAtor(){
        System.out.println("Qual o nome do ator?");
        var ator = leitura.nextLine();
        System.out.println("Qual avalição mínima para a série?");
        var avaliacao = leitura.nextDouble();
        List<Serie> serieBuscada = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(ator,avaliacao);
        System.out.println("Séries que o ator " + ator + " atua:");
        serieBuscada.forEach(s -> System.out.println("Série: " + s.getTitulo() + ", Avaliação: " + s.getAvaliacao()));
    }

    private void listarTop5series(){
        List<Serie> top5 = repositorio.findTop5ByOrderByAvaliacaoDesc();
        top5.forEach(s -> System.out.println("Série: " + s.getTitulo() + ", Avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriePorCategoria(){
        System.out.println("Digite a categoria/gênero desejada: ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Séries encontradas da categoria/gênero " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarSeriesPorTamanho(){
        System.out.println("Digite a quantidade de temporadas você quer que uma série tenha:");
        var totalTemporada = leitura.nextInt();
        System.out.println("Qual avalição mínima para a série?");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.seriesPorTemporadasEAvaliacao(totalTemporada,avaliacao);
        System.out.println("\nSéries encontradas com no maximo " + totalTemporada + " temporadas e avaliação maior ou igual a "+ avaliacao);
        seriesEncontradas.forEach(s -> System.out.println("Série: " + s.getTitulo() + ", Avaliação: " + s.getAvaliacao()));
    }

    private void buscarEpisodiosPorTrecho(){
        System.out.println("Diite um trecho do título do eísódio que busca: ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodios = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodios.forEach(e -> System.out.printf("Série: %s, Temporada: %s, Episódio: %s, Título: %s\n",
        e.getSerie().getTitulo(),e.getTemporada(), e.getNumeroEpisodio(),e.getTitulo()));
    }

    private void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(e -> System.out.printf("Série: %s Temporada %s - Episódio %s - %s Avaliação %s\n",
                    e.getSerie().getTitulo(), e.getTemporada(),
                    e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }

    private void buscarEpisodiosPorAno(){
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Digite a    partir de qual ano deseja buscar os episódios: ");
            var ano = leitura.nextInt();
            leitura.nextLine();
            List<Episodio> anoLancamento = repositorio.episodiosPorSerieEAno(serie,ano);
            anoLancamento.forEach(System.out::println);
        }
    }
}
