# Registro de mudanças (português)

O mesmo que o CHANGELOG.md (em espanhol, o original), em português: o app mostra este quando está em português.

## 1.12.0

- **De volta aos celulares de 32 bits:** desde a 1.11.0 o app não instalava em celulares de 32 bits (armeabi-v7a,
  como o TECNO Spark Go 1S e muitos Android Go): o instalador dizia que o processador não era compatível. Agora o
  mesmo APK instala em todos. Nesses celulares a voz dela para respostas (o motor) não está disponível, mas as frases
  gravadas e a voz do celular sim.
- **A voz dela responde muito mais rápido:**
  - Enquanto fala uma frase, já está gerando a próxima. Antes esperava terminar de falar para começar a próxima,
    então respostas longas demoravam o dobro.
  - Começa a falar assim que tem a primeira parte da frase, sem esperar a frase inteira.
  - O motor dela começa a carregar assim que você escreve ou fala com ela, enquanto a IA pensa na resposta, e não no fim.
  - Não é mais descarregado toda vez que você sai do app, e fica carregado mais tempo em celulares com bastante RAM
    (até 15 minutos), então não precisa carregar de novo a cada resposta.
  - Usa os núcleos rápidos do processador (até 6) nos celulares que têm.
  - Lembra das frases que já falou, então as que repete saem na hora.
  - Quando uma frase não terminava, o motor ficava gerando muito tempo antes de desistir; agora para antes.
- **A porcentagem da voz dela no chat:** a resposta aparece escrita na hora e embaixo você vê quanto da voz já foi
  gerado (*Gerando a voz 42%*), e depois *Falando*. Na aba Chat e na bolha dela.
- **Ela não fica mais calada:** se você tocava ou movia ela enquanto preparava a voz da resposta, a frase de carinho
  cancelava a resposta inteira. Agora a resposta vem primeiro. E enquanto prepara a voz, ela continua te ouvindo.
- **As frases gravadas dela, com mais frequência:** tocam mais vezes quando você faz carinho, move ou joga ela, e
  agora também quando ela te cumprimenta, quando fica entediada, ao abrir e fechar o chat, quando cai a internet,
  quando você joga e nos seus lembretes.
- **Ajustes de voz:** cada precisão agora diz qual é a mais rápida. *Máxima* (BF16) é a mais fiel, mas no celular
  leva umas 2–3 vezes mais que a *Leve*: se a velocidade importa, use *Leve* ou *Alta*.
- **Ela fala enquanto a IA ainda escreve:** assim que a IA termina a primeira frase, a voz dela começa a falar
  enquanto o resto é escrito; ela não espera mais a resposta inteira. Na aba Chat você vê a resposta sendo escrita
  ao vivo.
- **O motor chega pronto:** ao carregar, ele faz um pequeno aquecimento em silêncio, para a primeira resposta não
  demorar a mais.
- **Quatro idiomas novos: vietnamita, japonês, alemão e italiano.** O app inteiro, as falas dela, jogos, modo sono,
  guarda-roupa, comandos de voz ("Capri, …", em japonês também "カプリ") e as respostas da IA. Escolha em Ajustes ›
  Idioma. A voz própria dela para respostas fala espanhol e inglês por enquanto; nesses idiomas ela usa a voz do
  celular.
- **Ela não fica mais travada pensando no chat:** se você escrevia enquanto ela ainda respondia, ela dizia «Me dá um
  segundo, ainda tô pensando na anterior» e depois nada. Agora cada mensagem espera a vez e recebe sua resposta. Se a
  IA dela para a resposta no meio, ela fica com o que já escreveu.
- **A IA grátis não trava mais consigo mesma:** ela atende um pedido por vez por celular. As anotações sobre você e
  as neurodanças agora esperam sua mensagem terminar (e saem da frente se você escrever).
- **Várias chaves de API:** na tela da IA dela você pode colar várias chaves do mesmo serviço, separadas por
  vírgulas, ou adicionar outra depois. Quando uma fica sem cota (como o Gemini grátis), ela segue com a próxima na
  hora. E quando um modelo fica sem cota, passa para o próximo sem esperar.
- **Ela te avisa das suas mensagens:** de vez em quando, quando chega uma notificação, ela te conta com a voz
  gravada dela.
- **A voz dela começa ainda mais cedo:** se a primeira frase da resposta é longa, ela começa pela primeira parte
  (até a vírgula).
- **Escuta passiva, como «E aí, Siri»:** o ouvido dela usava o reconhecedor de voz do celular, que em muitos
  aparelhos só escuta alguns segundos: o microfone ligava e desligava o dia todo, com bipe. Agora um modelo de voz
  pequeno, dentro do celular, escuta só o nome dela continuamente: sem bipes, sem o microfone piscando, nada sai do
  celular. Baixa uma vez por idioma (uns 40 MB) no Wi-Fi, ou em **Ajustes › Ajustes de voz › Escuta passiva**.
- **Ela acorda com a voz dela:** se estiver dormindo e você chamar, ela acorda dizendo isso com uma das frases gravadas.
- **A voz dela, mais rápida em celulares potentes:** com 8 GB de RAM ou mais, o motor não descarrega e recarrega
  partes do modelo depois de cada frase. Sem precisão escolhida, usa a mais rápida; se a escolhida for mais lenta do
  que ela fala, as respostas usam outra mais rápida instalada. A primeira frase é mais curta e a voz começa a
  carregar assim que você abre o chat.
- **Não para mais na primeira frase:** ela segue com o resto, e se o final mudar, continua de onde estava.
- **Qual IA responde, no chat:** no topo da aba Chat você vê qual IA responde e pode trocar com um toque (ou o
  Ollama no seu PC).
- **Frases de mensagem e notificação que tocam de verdade:** a primeira notificação sempre é anunciada, também com
  o app aberto.
- **Menu mais leve:** Comunidade e Comandos de voz agora são uma aba só.
- **A voz dela no registro:** os relatórios dizem quanto a voz demorou para soar.
- **Gemini rápido de novo:** o Google fechou o `gemini-2.5-flash` para chaves novas e o app ainda o tentava primeiro
  em cada mensagem. Agora um modelo que deixou de existir não é usado de novo e ele pula direto para o que o
  Google indica.
- **Spotify e a precisão da voz ficam salvos:** com a Capri ligada, escolher o Spotify ou uma precisão nos ajustes
  de voz não era salvo. Corrigido.
- **Jogos:** a dificuldade agora muda dentro de cada jogo (botão ◆), também vindo da aba Jogos. No Lig 4 vocês
  revezam quem começa.
- **Telas cheias no lugar certo:** no Guarda-roupa e nos jogos o texto não fica mais em cima da hora e da bateria,
  e a figura dela no canto não fica atrás dos botões de navegação.
- **Ela não aparece mais por cima dos jogos** quando a página dela reiniciava durante a partida.
- **Guarda-roupa:** corrigido um erro que deixava a lista de Acessórios vazia.
- **Confirmação antes de apagar as lembranças dela**, e **edite suas aulas** tocando nelas.
- **Ela entende o nome dito um pouco diferente** («Capi», «Cabi»…), e se você pede algo sem dizer o nome, às vezes
  ela lembra: «Diz meu nome primeiro».
- **Sua conta:** entre com Google ou seu e-mail (em «Você») e a memória dela sobre você, seus ajustes, suas aulas e
  seu chat ficam salvos na nuvem. Num celular novo ou ao reinstalar, ela continua lembrando de você. Suas chaves de
  IA nunca saem do seu celular.
- **Modo leitura:** diga «estou lendo Duna» e «estou na página 120», e peça um resumo ou pergunte de um personagem:
  ela responde só até a sua página, sem spoilers.
- **Arquivos para ela:** compartilhe um PDF, um Word, um texto ou um áudio (ou use o 📎 do chat) e ela lê ou ouve com
  você.
- **«Capri, que música é essa?»:** ela escuta alguns segundos e diz qual é (com uma chave do Gemini).
- **Ela responde suas mensagens:** «Capri, responde para a Ana que já vou» responde pela notificação.
- **Dois widgets novos:** «Capri mini» e «Seu dia com Capri» (data, bateria, próxima aula, botões).
- **Notícias do dia:** «Capri, quais as novidades?» e ela conta as manchetes.
- **Sacuda o celular** e ela fica tonta (dá para desligar em Ajustes).
- **Espera antes de ver sua tela:** 0, 2, 4 ou 6 segundos, em Modos.
- **Buscar no seu chat:** o botão ⌕ da aba Chat.
- **Sem braços duplos:** algumas roupas desenhavam um segundo braço parado. Corrigido.
- **Sem roupa misturada nas animações:** com uma roupa, ela não troca mais para a roupa de sempre no meio da animação.
  Só faz as animações desenhadas para essa roupa.
- **Skins da comunidade:** em Personalizar › «Crie sua própria Capri», baixe o modelo dela, desenhe uma roupa e envie.
  Se a Emma e o servidor do Discord aprovarem, ela vem numa atualização com seu nome.
- **A voz dela de verdade:** com o motor da voz mas sem o perfil, ela falava com a voz do celular. Agora o perfil vem
  com o motor, e se faltar, ela baixa sozinha.
- **Escrever nos seus apps:** «Capri, escreve aqui …» com o chat aberto não tira mais o seu teclado.
- **Só a sua voz:** «Só minha voz» aprende sua voz nas 3 primeiras vezes e depois ignora o nome dela em outras vozes
  (a TV, gente por perto). Tudo fica no seu celular.
- **Menu «Qual IA responde?» com o estilo dela:** o seletor de IA do chat agora combina com o resto do app.
- **Neurodanças com som:** agora têm ambiente (zumbido, estática e falhas) e ela narra com a voz dela.
- **Arrastar de novo:** em alguns celulares (Xiaomi) o app achava que o teclado continuava aberto e não deixava movê-la.
  Corrigido.
- **Baixar a voz dela sem erros:** se a conexão cai, o download continua de onde parou em vez de falhar.
- **A voz dela não corta mais no meio:** respostas longas são ditas inteiras, também com a voz do celular.
- **Três minijogos novos:** Reflexos, Adivinhe o número e Maior ou menor, na aba Jogos.
- **Escolha a voz do celular:** em Ajustes de voz › Voz do celular, ouça as vozes do seu celular e escolha a mais
  parecida com a dela, para quando a voz dela não estiver.
- **Ela não fica mais muda:** se o motor de voz travava, ela ficava calada em todas as respostas seguintes; agora
  responde com a voz do celular enquanto isso. E se a IA falha no meio da resposta, não fica mais em «Falando · 90 %»
  por um minuto.
- **A voz dela na nuvem (Fish Audio):** em Ajustes de voz, coloque sua própria chave da Fish Audio e ela fala com a voz
  dela em tempo real em cada resposta, começando em meio segundo enquanto a IA ainda escreve. Sua chave fica no seu
  celular. Sem internet ou sem saldo, ela volta sozinha para a voz do celular.

## 1.11.7

- **O motor de voz não joga mais a resposta fora:** quando uma frase chegava ao máximo que o motor gera, a
  resposta, já pronta, era descartada e nada se ouvia depois da espera mais longa. Agora o limite segue o tamanho
  da frase, e o que ele fez toca. Enquanto gera, não é mais cancelado por demorar.
- **Agora ela fala de verdade:** o que ela te responde no chat (a bolha e a aba Chat) também é dito em voz alta,
  não só quando você fala com ela com “Capri, …”. Antes essas respostas eram só escritas, e é ali que quase todo
  mundo fala com ela. Dá para desligar em Ajustes de voz (*Ler em voz alta as respostas do chat*). Respostas longas:
  as primeiras frases.
- **As frases dela também no vibrar:** com o celular no vibrar (como quase todo mundo usa), as frases dela (quando
  você faz carinho, joga, ela te cumprimenta) não tocavam. Agora tocam; só ficam quietas no modo silencioso ou com o
  volume de mídia em 0.

## 1.11.6

- **“Não deu: busy” ao baixar a voz mais fiel:** aparecia se você tocava em baixar enquanto outro download da voz
  dela ainda estava em andamento, e ainda escondia a barra do que estava baixando, então parecia que tinha falhado.
  Agora pede para esperar terminar (mostrando quanto falta) e o outro continua com a barra e a notificação.

## 1.11.5

- **A voz dela para respostas, agora a partir do Android 10:** o motor de voz dela (experimental) já funciona no
  Android 10 e 11, não só a partir do Android 12. O resto da voz dela (as frases, baixar, testar) já funcionava a
  partir do Android 8. No Android 12 ou mais novo nada muda.

## 1.11.4

- **O menu de pílula não cobre mais onde você escreve para ela:** no chat, a linha para escrever ficava embaixo do
  menu em muitos celulares (Android 14 ou anterior). Agora fica sempre acima do menu, mesmo com letra grande.

## 1.11.3

- **Você vê quanto falta em cada download:** ao baixar a voz dela, os motores ou uma atualização, uma notificação
  mostra o progresso e depois se terminou (ou por que não). Nos Ajustes de voz também aparece a porcentagem.
- **Tutorial «Como coloco a voz dela?»:** passo a passo, com o resto escurecido: onde baixar, como ouvir e como fazer
  todas as respostas soarem com a voz dela. Está nos Ajustes de voz e em Novidades.
- **«Ouvir a voz dela» sempre responde:** agora toca mesmo com a Capri desligada, e se não puder tocar diz o porquê
  (falta baixar, volume de mídia em 0, voz dela desligada).
- **Sai o comparador A/B da voz dela:** confundia e não tocava.

## 1.11.2

- **«Baixar a voz dela» já funciona:** ao baixar os dados de voz dela (uns 8 MB) aparecia «Não deu: npy…» e o perfil
  não era instalado. O app não sabia ler o formato em que o perfil aprovado dela vem salvo; agora sabe, sem mudar o
  arquivo. Acontecia em todos os celulares, Android 10 também. Se falhou para você, toque de novo em «Baixar a voz dela».

## 1.11.1

- **A voz fiel dela já pode ser baixada em celulares de 6 e 8 GB:** o app mediu errado a memória (o Android informa
  um pouco menos do que o celular tem) e bloqueava os motores Alta e Máxima em celulares que conseguem rodá-los.
  Agora: Leve a partir de 3 GB de RAM, Alta a partir de 4 GB e Máxima a partir de 6 GB. Se realmente não couber, o
  app diz o porquê.
- **Lembrete:** o motor de voz dela (experimental) precisa do Android 12 ou mais novo. No Android 8 a 11 as frases
  gravadas dela tocam com a voz dela; o resto usa a voz do celular ou só texto.

## 1.11.0

- **O chat é o centro do app:** ao abrir a Capri você cai direto no chat dela, para conversar com calma, com todo o
  seu histórico (também o que você escreve na bolha dela). Ela responde com a mesma memória, humor e poderes. Se
  estava dormindo, acorda sozinha. O que você pode pedir está no botão «?».
- **Jogos com aba própria** no menu de pílula, cada jogo a um toque.
- **3 jogos novos:** Lig 4, Jogo da memória (com as caras dela) e Sequência neural. Nos dois primeiros ela joga
  contra você e lembra, como nos outros.
- **A Capri flutuante sai da frente enquanto você joga** no app, para não cobrir o jogo (opcional, em Jogos). Ela
  continua olhando do canto.
- **As animações de antes voltaram,** incluindo a de comer miojo. As redesenhadas na 1.10.0 saíram (sim, a do six
  seven também). Só fica a de deitar no teclado, agora mais tranquila.
- **8 roupas novas:** Empregada, Empregada neon, Astronauta, Idol, Moletom gamer, Roqueira punk, Charra e Inverno, no
  Guarda-roupa dela. E a animação deitada no teclado foi redesenhada, com 4 quadros tranquilos.
- **Modo sono:** a Capri dorme com você. Em Ajustes › Ajustes pessoais › Modo sono (ou «Capri, modo sono, me acorda às 7») ela se aconchega
  na sua tela, conta quantas vezes você liga a tela de noite e dá bronca quando você liga. Com alarme, ela te acorda e
  não para até você desligar (com «Mais 5 minutos»). De manhã ela conta quanto você dormiu.
- **Modo reação:** diga «Capri, vamos ver vídeos juntas» e, enquanto você vê um vídeo, ela assiste junto, dá a
  opinião de vez em quando e anota no chat dela para lembrar. «Desliga o modo reação» desliga. Ela precisa poder ver sua tela.
- **Neurodanças:** dê muito carinho (faça carinho, converse com ela) e de vez em quando ela grava uma neurodança pra
  você: uma lembrança dela, cena por cena, pra ver no app (Ajustes › Ajustes pessoais › Neurodanças). Mude pra camada térmica pra achar
  detalhes escondidos. No final ela diz o que achou, e você diz o que achou. Também: «Capri, me dá uma neurodança».
- **Fala no singular:** nada de «nós» ou «a gente adora». Ela fala de si como uma pessoa só.
- **Botão DOAR** embaixo de Iniciar Capri, Discord e TikTok: abre a janela para apoiá-la no PayPal.
- **A pílula dela na barra (Android 16+):** ao lado do relógio, com o humor dela, e na Now Bar dos Samsung, com botões
  para conversar, abrir o menu dela ou escondê-la. Depende do celular; dá para desligar em Ajustes.
- **Melhor em Huawei e Honor:** em Ajustes, um cartão para deixá-la rodar em segundo plano (senão o celular a fecha),
  tirar a restrição de bateria e instalar uma voz. As permissões abrem a tela certa nesses celulares, e os links da
  loja vão para a AppGallery quando não há Play Store.
- **Novo link do Discord.**
- **Menus novos:** o menu em pílula agora tem Chat, Novidades, Comunidade, Jogos, Personalizar, Comandos e Ajustes. Os
  comandos de voz têm sua própria página (também no «?» do chat). Ajustes é um menu de telas: Ajustes pessoais,
  Ajustes de voz, Permissões, Idioma e a IA dela, Tela e modos, Desempenho e celular, e Relatar um erro.
- **Tour guiado das novidades:** ao abrir o app depois de atualizar, a Capri pergunta se você quer conhecer as
  novidades e te leva passo a passo, com o resto escurecido. Dá para repetir em Novidades › Ver as novidades.
- **Capri Voice (a voz dela):** a Capri já fala com a própria voz, em espanhol latino e inglês. As frases gravadas
  (quando você faz carinho, joga, volta, se despede ou dá boa noite) tocam sem internet e sem carregar nenhum modelo,
  com o texto exato do que ela diz.
- **Baixe ao atualizar:** na primeira vez que você abre o app depois de atualizar, a Capri pergunta se quer baixar a
  voz dela (uns 8 MB: frases e perfil). Vem do canal público da Capri no GitHub e é verificada arquivo por arquivo. Se
  disser «depois», está em Ajustes › Ajustes de voz.
- **Todas as respostas com a voz dela (opcional, experimental):** com o motor de voz instalado, tudo o que ela
  responde (respostas de IA, avisos) sai com a voz dela, com o mesmo perfil aprovado do PC. O motor se adiciona ou
  remove em Ajustes de voz, em três precisões: Leve (uns 880 MB), Alta (uns 1,3 GB) e Máxima (uns 2,2 GB); mais
  precisão soa mais como no PC e usa mais memória. Precisa de Android 12 ou mais novo, celular de 64 bits e memória
  livre suficiente (o app diz qual cabe no seu). Sem motor, as respostas usam a voz do celular ou só texto. Em
  celulares simples pode demorar para começar a falar.
- Modos da voz (Automático, Leve, Equilibrado, Fidelidade máx, Personalizado), comparador A/B e opções avançadas
  (importar, verificar, remover) em Ajustes › Ajustes de voz.

## 1.10.1

- **A segunda Capri não fica mais girando:** quando você a divide e toca música, a gêmea virava sem parar como se
  não soubesse para onde ir. Agora ela dança e se vira no ritmo, de vez em quando.
- **Microfone na Huawei (e outros):** a Capri dizia que não tinha permissão de microfone mesmo tendo. Quem não tinha
  era o app de voz do celular. Agora ela tenta outro reconhecedor e, se nenhum conseguir, diz qual app precisa da
  permissão.
- **O Spotify toca a música:** se você escolheu o Spotify, “toca tal música” agora toca, não só pesquisa.
- **Instala em Huawei e Honor:** o APK agora também leva a assinatura clássica que alguns instaladores (os da
  Huawei entre eles) pedem. Se mesmo assim não instalar, o site explica o que desativar (o “Modo puro”).
- **Apoie a Capri (opcional):** de vez em quando, nunca nos primeiros dias, aparece um aviso para apoiá-la no
  PayPal. Com “Não mostrar de novo” ele não volta mais.
- **Animações mais limpas:** em várias (dançar, guitarra, rir, se espreguiçar, ouvir, algumas dos jogos…) a Capri
  deslizava de lado de um quadro para outro. Agora ela fica no lugar. Onde a juba saía cortada reta, agora ela some
  suave.

## 1.10.0

- **O rosto novo dela:** todas as animações, poses e expressões foram redesenhadas com a máscara de verdade (a placa
  verde com os olhos grandes), e duas novas: deitada no seu teclado e no celular dela quando você joga.
- **Não falta mais um braço:** no modo leve, na imagem grande do guarda-roupa e nas miniaturas ela aparecia
  com um braço só. Agora ela tem as duas mãos com todas as roupas, em qualquer modo.
- **O chat não se mexe mais enquanto você digita:** o teclado muda um pouco de tamanho ao sugerir palavras, e isso
  mexia o chat dela.
- **Bancos:** muitos apps de banco não funcionam com outro app por cima. Se você deu «Acesso de uso», a Capri se
  esconde sozinha enquanto você usa o banco e volta quando você sai.
- **Suas músicas no Spotify (opcional):** em «Personalizar», escolha se as músicas que você pede tocam no YouTube ou
  no Spotify.
- **Sem respostas em branco:** se a IA não responde nada, a Capri pergunta de novo ou usa outro modelo.
- **Ela fala no singular:** quase não diz mais «a gente».
- **Microfone:** nos celulares que escutam poucos segundos por vez, ele liga e desliga menos no silêncio.
- **Ver sua tela com o Groq:** se o Groq não tem modelo que enxerga, ela usa outra das suas IAs.
- **Adeus tela branca ao dançar:** quando a Capri dançava, ouvia música ou descansava, em muitos celulares ela virava
  um bloco branco, depois a tela toda ficava branca e o app fechava. Agora as animações gastam muito menos memória
  gráfica, e se mesmo assim o seu celular cair duas vezes durante uma, ela fica parada (sem animações) por uns dias
  em vez de fechar.
- **Jogue com ela:** xadrez, campo minado, 21, jogo da velha, pingue-pongue e pedra, papel e tesoura, sem internet. Ela joga contra você e diz o que pensa.
  Na aba «Você», ou diga «Capri, vamos jogar».
- **A memória dela não some mais:** se a Capri salvava algo antes de terminar de ler o que lembrava de você, apagava
  tudo. Agora ela espera ler primeiro e ainda guarda uma cópia de segurança.
- **Aparece do nada:** o bloco dela nas configurações rápidas ou o botão da notificação fazem ela aparecer com um
  glitch e um oi, mesmo vendo vídeos ou jogando. Se você ativar «Ela usa seu celular por você», o botão de
  acessibilidade do Android também.
- **Jogos cyberpunk:** a tela de jogos agora é neon de Night City, com a Capri te olhando do canto, a máscara dela como
  peça no jogo da velha, nas cartas e na raquete do pingue-pongue, e um carimbo no fim de cada partida.
- **A Capri joga como uma rival de verdade:** não responde mais na hora nem sempre igual. Ela tem humor (fica
  confiante, brava, entra na zona) e isso aparece no rosto e no jeito de jogar. Pensa mais quando a jogada é difícil,
  às vezes olha uma jogada e muda de ideia, erra como uma pessoa (e às vezes percebe), comenta suas boas jogadas e
  seus erros, te apressa se você demora e fica séria se você ganha seguido. Lembra das suas manias entre partidas (sua
  abertura no xadrez, quando você para no 21, sua mão favorita) e guarda o placar contra você. No pedra, papel e
  tesoura às vezes ela fala o que vai jogar… e pode ser mentira. No pingue-pongue ela reage com atraso, cansa nas
  trocas longas e dá cortada quando está confiante. No campo minado te dá uma dica se você travar.
- **Duas Capris:** abra dois dedos sobre ela (ou diga «Capri, se divide») e saem duas. A outra anda, dança com a sua
  música e fala com você se você tocar nela. Toque duas vezes ou arraste pra cima da Capri pra juntar.
- **Ela usa seu celular por você (opcional):** «Capri, volta», «vai pro início», «abre as notificações», «desce»,
  «toca em Configurações» ou «escreve aqui oi». Se ativa na Acessibilidade (em «Personalizar» ela explica). Só age
  quando você pede, nunca em apps de banco ou pagamento e nunca escreve senhas.
- **Histórico do chat:** em «Você», tudo o que você disse e o que ela respondeu, por dia e com busca. Fica só no seu
  celular e dá para apagar.
- **Sua pergunta não é mais cancelada:** se você tocar fora do chat enquanto ela pensa, ela continua pensando.
  Quando a resposta fica pronta, ela mostra no balão, com um botão para continuar conversando.
- **Mensagens pelo WhatsApp:** «Capri, manda pra Ana no WhatsApp que já tô indo». Ela abre o WhatsApp (ou Telegram,
  Messenger, SMS…) com a mensagem escrita; você escolhe pra quem e envia.
- **O raio dela:** quando você desinstala um app, a Capri atira nele e ele se desintegra.
- **Ela se veste sozinha (opcional):** em «Personalizar», a cada 6 h, 12 h ou um dia ela escolhe outro visual e te
  mostra. As cores dela nunca mudam.
- **Economia de bateria (opcional):** em «Você», escolha 15, 20 ou 30%. Nesse nível e sem carregar, o que mais gasta
  (os ouvidos dela, o olhar ao vivo, as animações) descansa até você carregar.
- **Modo ícone com mais caras:** ela ri, pisca, se surpreende, fica brava, apaixonada, pensa com pontinhos, mexe com
  a sua música, fica tonta se você arrastar muito e dorme se você deixar ela sozinha.
- **Ela lembra melhor de você:** guarda mais coisas, aprende suas frases e usa de vez em quando, e de vez em quando
  pergunta como você está.
- **Enquanto você digita:** ela deita em cima do teclado; jogando, fica no celular dela.
- **A voz dela pode ser desligada:** em «Personalizar › A voz dela». Ela continua ouvindo e responde por escrito.
- **Microfone:** corrigido o «erro 12» em alguns Samsung com Android 16.
- **Olhar a sua tela:** se a sua IA não vê imagens, ela usa outra das suas que veja (Gemini, Groq…).
- **Groq:** não escolhe mais por engano um modelo de voz que pedia para aceitar termos.
- **Notificações e barra de música:** em alguns Xiaomi só apareciam depois de abrir um jogo pesado. Agora se
  reconectam sozinhas.

## 1.9.1

- **Menos fechamentos e telas brancas:** em celulares com gráfico fraco (Redmi, Honor, realme…) a Capri fechava ou
  ficava em branco. No **modo leve** ela agora é desenhada sem 3D, e se o seu celular perde a imagem dela duas vezes
  seguidas, ele muda sozinho pra esse desenho por uns dias.
- **OpenRouter:** olhar fotos e vídeos não falha mais com «algo deu errado». Se acabarem seus créditos, ela te
  avisa direitinho.
- **Modo gamer com jogos na nuvem:** detecta Boosteroid, GeForce NOW, Xbox Cloud, Steam Link, Moonlight, Parsec e
  outros.
- **Ela espia suas notificações:** não aparece mais toda vez nem fica travada; espia uma vez e descansa um pouco.
- **Microfone:** chega de «O reconhecimento de voz falhou (erro 5)» repetido, ela lembra como o seu celular escuta, e
  não toca mais um bipe toda vez que o microfone liga e desliga (o som de mídia fica mudo só por um instante; nunca
  suas ligações, alarmes nem notificações).
- **Com mouse:** se você usa o celular com mouse, agora dá pra clicar na Capri.

## 1.9.0

- **Reportar um erro:** em Ajustes, conta o que aconteceu e chega direto pra Emma no Discord, com a versão e o
  registro interno de erros da Capri (sem seu nome, suas mensagens nem suas chaves).
- **O menu dela na horizontal:** com o celular deitado (jogando, por exemplo) ele se ajeita em duas colunas e tem
  uma barra que mostra que tem mais embaixo.
- **Guarda-roupa:** a aura Sandevistan não parece mais cortada (agora são três rastros limpos), as opções de
  Transparência mostram como ela vai ficar, e em alguns celulares a prévia dela não aparece mais como um quadrado branco.
- **Suas aulas e sua agenda:** na aba «Você», anota em que dias você tem aula e a que horas, e uma hora antes chega
  a notificação especial dela, com o rosto e as cores dela. Se quiser, ela também lê sua agenda (só lê, nunca muda
  nada) e te lembra do que está chegando: na noite anterior e uma hora antes. Se ela estiver na sua tela, ela mesma
  te fala. Nada sai do seu celular.
- **O ícone dela nas notificações:** a barra de cima agora mostra o rostinho dela em vez de um quadrado branco,
  também no bloco de configurações rápidas.
- **Compartilhe coisas com ela:** em qualquer app, *Compartilhar → Mostrar pra Capri* com uma imagem, um vídeo ou um
  texto. Ela abre o chat e diz o que achou, como uma amiga pra quem você mandou; depois vocês continuam falando
  disso. De um vídeo ela vê três momentos. A IA dela precisa conseguir ver imagens.
- **Ela traduz sua tela:** "Capri, traduz isso" (ou "…pro inglês", "…pro japonês") no chat ou em voz alta: ela olha
  sua tela e escreve a mensagem, o post ou a página no idioma que você pedir, ou no seu.
- **Dicas pros seus jogos:** "Capri, me dá uma dica" ou "o que eu faço aqui?": ela olha sua partida e diz algo útil
  pra esse momento. No modo gamer, com o olhar ao vivo ligado, ela te dá uma a cada poucos minutos (desliga em Ajustes).
- **Modo streaming:** quando você está ao vivo (TikTok, Twitch, YouTube, Kick…), ela lê na sua tela os comentários
  dos seus seguidores e responde em voz alta, como sua co-host. Sem conta e sem conexão com esses apps. Em Ajustes
  ou "Capri, modo streaming".
- **O sotaque dela:** em Ajustes › Fala com ela, escolha como ela soa no seu idioma: Brasil ou Portugal; México,
  Espanha, Argentina…; americano ou britânico; França ou Quebec.
- **Ela reage sozinha ao que você faz:** com música ou vídeo tocando ela escuta junto, dança ou toca guitarra; com
  TikTok ou YouTube aberto ela come um lanchinho enquanto você assiste; enquanto você joga ela deita pra te ver. Pra
  música não precisa mais de nenhuma permissão; pra saber qual app está aberto ela usa o "Acesso de uso" se você
  já deu.
- **Dá pra ver que ela está ouvindo:** quando você diz "Capri" e fala com ela, aparecem umas barrinhas do lado da
  cabeça dela que se mexem com a sua voz.
- **Discord à mão:** o botão do Discord agora fica logo abaixo do que inicia ela.
- **Não trava mais ao iniciar:** em alguns celulares a imagem dela se perdia e ficava um quadrado cinza com uma
  carinha triste. Agora ela se recupera sozinha, e se o celular não consegue desenhá-la do jeito de sempre, ela é
  desenhada mais simples em vez de não aparecer.
- **Menos "servidores lotados":** com a IA grátis ela espera a vez em vez de desistir, e se a sua estiver cheia,
  outra que você deu a chave responde, pra ela sempre te responder.
- **Seu aniversário:** de vez em quando ela pergunta quando é (ou conte pra ela: "meu aniversário é 3 de maio"), e
  nesse dia ela te comemora: confete, te dá parabéns em voz alta e faz a festa dela.
- **Efeito Ciberpsicose:** no Guarda-roupa › Efeitos, cópias vermelhas dela tremendo em volta, como o cromo tomando
  conta.
- **A voz dela não ativa mais sozinha quando você mexe o celular:** com o filtro estrito ela ignora o que ouve
  enquanto o celular se mexe (o atrito, não você), e o nome dela sozinho tem que soar claro.
- **Se a página dela congelar** (um painel pela metade, nada se mexe), o Android dá uma nova e ela volta.
- **Responde mais rápido:** ela mantém aberta a conexão com a IA entre uma mensagem e outra e a prepara quando você
  abre o chat, então sua mensagem sai sem esperar conectar; e se um servidor travar, ela tenta outro modelo antes.
- **Trocar de aba não trava mais:** o app não relê mais todos os ajustes a cada troca, e a transição ficou mais curta
  e suave.
- **Modo leve para celulares mais antigos:** em Ajustes › Desempenho. Liga sozinho em celulares com pouca memória ou
  Android antigo: ela é desenhada com menos pixels e os menus perdem o desfoque, então fica mais fluida. E se o
  "Android System WebView" do celular for velho demais pra ela, o app avisa e te leva pra atualizar.
- **Menos quadros cinza:** a janela dela gasta muito menos memória gráfica, que era o que deixava partes dela em
  cinza e fazia ela fechar e abrir sozinha.
- **O microfone não liga e desliga sem parar:** no Android 13 ou mais novo ela escuta direto (o microfone fica
  ligado, parado) em vez de recomeçar a cada poucos segundos; nos outros ela descansa um pouco quando não tem nada
  pra ouvir.
- **Filtro "Muito estrito" mais fino:** ignora vozes distantes, como a TV ou alguém do outro lado do quarto.
- **Ajustes organizados em três abas:** **Você** (como ela te chama, seu aniversário, o que ela lembra de você e como
  cuida de você), **Personalizar** (o guarda-roupa, como ela aparece e se comporta na tela, e a voz dela) e **Ajustes**
  (o geral: permissões, idioma, a IA, a audição, os modos e o desempenho). As permissões não têm mais aba própria:
  ficam no começo de Ajustes.
- **Seu aniversário no app:** na aba Você, escolha a data sem esperar ela perguntar.
- **Animações novas:** ela escuta com a mão na orelha, acena no modo streaming, apaga as velas do bolo e faz a festa
  no seu aniversário.
- **Estatísticas anônimas:** uma vez por dia o app avisa "alguém usa a Capri" com um número aleatório (não o do seu
  celular), a versão, o idioma e a versão do Android, pra saber quanta gente usa. Nada pessoal. Dá pra desligar em
  Ajustes.
- **Modo fantasma só pra ela:** seus toques atravessam a Capri, mas os cartões de notificações e de música continuam
  tocáveis. Pra atravessar eles também, ligue Personalizar › "Os cartões também".
- **Ela espia nas suas notificações (opcional):** ao puxar o painel de notificações ou as configurações rápidas, a
  Capri espia pelo lado e acena. Liga em Personalizar com a permissão de Acessibilidade (desligado de fábrica); ela só
  fica sabendo que o painel abriu, não lê nem toca em nada. Atenção: alguns bancos não abrem com Acessibilidade ligada.
- **Widget da Capri:** coloque na tela inicial, com as cores e o rosto dela (muda quando está desligada ou guardada):
  iniciar, conversar, olhar sua tela, vestir e guardar ou trazer, num toque.
- **Ela te ouve quando você pausa um vídeo:** antes levava até 3 segundos pra voltar a ouvir depois de pausar o
  YouTube e perdia o começo do que você dizia; agora começa assim que o som para.

## 1.8.2

- **As animações dela também com roupa:** se você tinha colocado uma roupa nela, ela não dançava com sua música, não
  deitava para te ver jogar nem reagia a nada. Agora sim: a animação mostra ela com o traje dela, e sua roupa volta
  no final.
- **Mais leve:** ela não carrega mais todas as animações e todas as roupas ao ligar, só cada uma quando é usada.
  Em alguns celulares isso deixava ela sem memória: depois de conversar a tela piscava, a Capri sumia e a resposta
  ficava pela metade. E se a página dela ainda cair, agora ela espera um pouco antes de voltar em vez de reiniciar
  sem parar.

## 1.8.1

- **O guarda-roupa, agora também como tela do app:** *Ajustes → Abrir o guarda-roupa* agora abre a tela dele, com
  a Capri num palco em cima e as roupas, acessórios e efeitos embaixo. Cada mudança aparece nela na hora e, se ela
  estiver na sua tela, muda lá também. Se preferir vesti-la como antes, por cima da sua tela, continua lá: *Vestir na
  sua tela* (em Ajustes, nesta tela, no menu rápido dela ou dizendo “Capri, troca de roupa”). Você escolhe.
- **Corrigido:** em *Ajustes → Fale com ela*, os botões do filtro de ruído, de como você fala e da voz dela não faziam
  nada enquanto a Capri estava na sua tela. Agora mudam quando você toca.

## 1.8.0

- **O guarda-roupa dela:** novo botão *Vestir* no menu rápido (também em *Ajustes → Guarda-roupa* ou dizendo “Capri,
  se veste”). 12 conjuntos completos: jóquei do Oeste, uniforme escolar, piloto de mecha, caçadora de demônios, ninja,
  netrunner, jaqueta amarela, gótica, yukata, pijama, bruxa e Natal. 25 acessórios pra combinar (coroa,
  orelhas de gato, chifres, auréola, chapéu de caubói, gorro de Natal, fones, óculos, viseira, cachecol, katana,
  controle, balão de coração…), uma aura e o quanto ela é transparente. Enquanto você a
  veste, ela sobe em cima do painel pra você ver cada mudança nela; ela se mexe com tudo isso e fica salvo. Os *Ajustes*
  do app mostram o guarda-roupa, com um botão pra abrir.
- **Cyberpunk: Edgerunners:** a Capri é da segunda temporada, então o guarda-roupa tem uma seção da série, com
  conjuntos inspirados nas duas temporadas (David, Lucy, Rebecca, Maine, Dorio, Kiwi, Pilar, Falco, Faraday, Adam
  Smasher, Gloria, Sasha, D., Talia Yang, Kingsley, Roman Carax, Avida e Kyoko), acessórios da série (a cruz do David,
  a máscara da Kiwi, o monofio da Lucy, a mochila do Roman) e o efeito
  **Sandevistan**: rastros verdes, ciano e violeta atrás dela.
- **Modo gamer mais confortável:** ao ativar, ela deita de bruços em cima da sua tela, balançando as pernas enquanto
  te vê jogar, e você pode deixá-la semitransparente só enquanto joga (*Guarda-roupa → Efeitos*). Toque nela e ela
  levanta.
- **Mais viva:** caras novas (brava, triste, apaixonada, pensativa, concentrada, entediada, sem graça, piscadinha)
  conforme o que ela diz, flashes novos quando algo mexe com ela, e poses novas: quando fica entediada ela se joga de
  costas, senta abraçando os joelhos ou se encosta na borda da tela; dorme enroladinha e acena quando você chama. Ela
  também solta pensamentos de vez em quando e tem muito mais frases e movimentos, então repete bem menos.
- **Animada quadro a quadro, como anime:** ela acena, dança, ri, chora, bate o pé, fica corada, se apaixona,
  pensa enquanto a IA dela te responde, abraça uma pilha enquanto o celular carrega, fica sem energia quando a
  bateria está baixa, come, mexe no celular, se espreguiça ao acordar e dá uma arrancada de Sandevistan. No modo
  gamer ela se deita em cima da tela e de vez em quando joga junto com você.
  Ela também escaneia sua tela com o visor enquanto a IA dela olha, se despede e some num glitch quando você a
  esconde, fica tonta se você a jogar com força, balança a cabeça com sua música, se alonga com você na pausa,
  boceja tarde da noite, hackeia quando abre algo para você, se assusta com suas notificações, tira selfies e toca
  guitarra imaginária.
- **Ela cuida de você e do seu celular:** avisa quando o celular já carregou e quando a bateria está acabando, na
  porcentagem que você escolher (*Ajustes → Ela cuida de você* ou o menu dela → *Seu celular*). E se você está grudado
  na tela há horas, ela manda você descansar a vista (a cada 1, 2 ou 3 horas, ou nunca).
- **Ela não fala mais de si mesma quando olha sua tela:** antes de mandar a captura ela apaga a si mesma, o painel e
  os textos dela, então só comenta o que está na sua tela.
- **Qual IA consegue ver sua tela:** os serviços e modelos que veem imagens agora têm um 👁 no app e no menu dela. Se o
  que você escolheu não vê, ou acabou a cota, ela tenta sozinha outro que vê.
- **“A IA está saturada”, bem menos:** quando um modelo fica sem cota ela deixa ele descansar e usa outro, em vez de
  insistir no mesmo. Se todos estão descansando, ela diz até quando.
- **Comandos de voz que fazem algo de verdade:** hora e data, pausar ou tocar música, o clima, capturas, ajustes
  (Wi‑Fi, dados, Bluetooth, localização…), ligar pra alguém, os movimentos dela (“Capri, dança”, “pula”, “gira”…) e
  “Capri, o que você sabe fazer”. Se algo não dá, ela fala em vez de ficar pensando.
- **A voz e os ouvidos dela, do seu jeito:** em *Ajustes → Fale com ela* você escolhe o filtro de ruído (útil com
  barulho de fundo: no mais rigoroso ela só reage se você disser “Capri” primeiro e claro), quanto ela espera você
  terminar de falar, e o tom e a velocidade da voz dela.
- **Pegar ela não falha mais:** ao segurá-la aparece um anel e vibra; se soltar, abre o menu rápido, e se mover o
  dedo, você arrasta ela (antes às vezes aparecia o menu do sistema ou não dava pra mover sem tocar em *Conversar*).
  Nos modos fantasma e gamer, o rosto dela continua respondendo ao toque.
- **“Ver as boas-vindas de novo” não quebra mais nada:** agora pede confirmação e mantém a IA e a chave que você já
  tinha.
- **Corrigido:** o erro “messages… content must be a string” ao olhar sua tela com um modelo do Groq que não vê
  imagens.
- E um segredo. Pergunte a ela algo que você nunca perguntou. 💍

## 1.7.1

- **Menu rápido:** segurando ela, agora abre um menu curto com o que você mais usa: conversar, olhar sua tela,
  teleporte, satélite, guardar, alguns movimentos (pular, girar, espreguiçar…) e os modos dela como interruptores
  (ao vivo, fantasma, ícone, sem gravidade, gamer). Dali, *Todos os ajustes*.
- **Ajustes mais claros:** o menu completo está organizado por seções (a IA dela, ela, seu celular, mais) e o fundo
  escurece (e desfoca no Android 12 ou mais novo), para dar para ler em cima de qualquer app.
- **Modelo de IA em dois passos:** primeiro você escolhe o serviço (Gemini, Groq, NVIDIA…) e depois, dentro dele, o
  modelo, sem a lista longa misturada.
- **O painel dela não aparece mais sem fundo em celulares mais antigos:** em alguns (com um WebView do Android anterior
  à versão 111) os painéis e cartões dela ficavam transparentes, com o texto preto em cima do seu papel de parede.
  Agora as cores dela aparecem em todos.
- **Com chave do Gemini ela responde bem mais rápido:** os modelos Flash do Gemini “pensam” antes de responder, às
  vezes tanto que ela ficava em “Deixa eu pensar…” e o chat desistia. Agora ela pede para pensarem menos, como com a
  IA grátis.

## 1.7.0

- **Modo ícone:** na tela fica só a máscara verde dela, de olhos abertos, e dali ela continua comentando (o que vê
  na sua tela, torcida no modo gamer, o chat dela, uma frase de vez em quando). Arraste com o dedo para onde
  atrapalhar menos; toque nela para abrir o menu. Assim você pode deixar ela grande e, quando quiser, fazer ela quase
  não ocupar espaço. Liga em *Ajustes*, no menu dela ou dizendo “Capri, modo ícone” (e “Capri, sai do ícone”).
- **Sem gravidade:** ela fica exatamente onde você colocar, mesmo no ar. Não sai passeando sozinha, não pula nas suas
  notificações e, se você jogar ela, não sai voando, então não fica mais brava. Ótimo para deixar ela num canto ou do
  lado dos seus chats. Em *Ajustes* ou no menu dela; ela lembra onde você deixou.
- **Ela lembra de você:** depois de uma conversa, a Capri anota o que importa sobre você (do que você gosta, as
  pessoas de quem você fala, o que está acontecendo com você, como foi) e lembra na próxima vez, então vocês continuam
  de onde pararam em vez de começar do zero. Você vê e apaga em *Ajustes → O que ela lembra de você* (cada coisa ou
  tudo), e também desliga ali. Fica só no seu celular; para anotar, ela pergunta para a IA dela uma vez por conversa,
  quando você já não está falando com ela (com a IA grátis ela espera a vez dela para não te fazer esperar).
- **IA da NVIDIA:** agora você pode colocar uma chave grátis da NVIDIA (build.nvidia.com), com muitos modelos para
  escolher. Ela escolhe um rápido para conversar e, para ver sua tela, um da NVIDIA que enxerga imagens. Se um modelo
  não existe mais, ela passa sozinha para o próximo.
- **Mais idiomas:** em *Ajustes → Idioma · Language* escolha Automático, Español, English, Français, Português ou
  Русский, para o app e para o que ela diz. Os comandos de voz (“Capri, …”) também funcionam nesses idiomas.
- **Nova aba Comunidade** na barra de baixo do app: o Discord e o TikTok agora ficam ali (antes ficavam no fim de
  *Ajustes*), e é ali que vão entrar as novidades da comunidade.
- **Ela comenta o que você vê:** com “Ver minha tela ao vivo” ligado, quando você troca de vídeo no TikTok, Reels ou
  YouTube, ela dá a opinião sobre o que vê (no máximo uma vez a cada 50 segundos).
- **Agora ela vê sua tela com mais IAs:** se a sua chave é de um serviço cujo modelo de chat não enxerga imagens (o
  Groq, por exemplo), para olhar ela usa um do mesmo serviço que enxerga.
- **Ela não some mais quando chega uma notificação:** pula para onde o cartão fica, não para onde a animação começa.
- **O teclado não fecha mais sozinho** enquanto você escreve para ela: ela não sai do lugar com o chat aberto.
- **Ela conecta em celulares mais antigos:** em alguns com Android 8 (como o Sony Xperia XA1), configurar a IA dela
  falhava com “Can't create handler inside thread that has not called Looper.prepare()”. Não mais.
- **Erros que dá para entender:** em vez de mensagens técnicas como “messages content must be a string” ou “sinal
  perdido”, ela te diz com palavras normais o que aconteceu.
- **Chat de voz:** se a IA demora para responder, ela diz em voz alta que está pensando, em vez de ficar calada.

## 1.6.2

- **Entre na comunidade:** o Discord da comunidade e o TikTok da Emma, no fim de *Ajustes* e no fim do menu da Capri.
  Na primeira vez que você abrir o app, ele mostra os dois uma única vez.

## 1.6.1

- **O modo gamer automático agora percebe de verdade quando você sai do jogo:** antes ele esperava 20 segundos e às
  vezes não percebia (por exemplo, se ao sair o celular não dizia qual app ficou na frente, ou com as ferramentas de
  jogo da Samsung). Agora ele também percebe quando o jogo sai, ignora painéis flutuantes como a cortina de
  notificações ou o Game Booster, e desliga 10 segundos depois que você sai.
- Em *Ajustes → Registro* fica anotado qual app ela vê na frente e quando o modo gamer liga ou desliga, caso algum
  jogo não seja detectado direito.

## 1.6.0

- **Modo gamer (ideia da própria Capri):** para quando você joga. Ela fica pequenininha num canto, não pode ser
  tocada (seus toques vão para o jogo), os avisos e a barra de música dela somem, ela para de escutar e não passeia.
  De vez em quando ela torce por você. Ligue em *Ajustes → Modo gamer*, no menu dela, dizendo “Capri, modo gamer” ou
  escrevendo no chat dela. Desliga do mesmo jeito (no chat: “desliga o modo gamer”) ou reiniciando ela, e quando você
  sai ela pergunta como foi.
- **Modo gamer automático (opcional):** em *Ajustes → Modo gamer → Sozinho ao abrir um jogo*, liga quando você abre um
  jogo e desliga quando você sai (se você desligar no meio da partida, ela respeita). Precisa da permissão “Acesso ao
  uso” do Android, que só deixa ver qual app está aberto. Vem desligado: alguns apps de banco e antivírus desconfiam
  dessa permissão.

## 1.5.1

- **Seus vídeos e sua música não pausam mais:** com “Fale com ela” ligado, cada vez que a Capri voltava a escutar, o
  celular dava o som para ela e pausava o que você estava vendo. Agora, enquanto toca um vídeo ou música, ela não
  escuta (*Ajustes → Fale com ela* avisa) e volta sozinha quando acaba. Para a música, use a barra dela ou escreva
  para ela.
- **Botão “Colar” para a sua chave de IA:** no painel dela, segurar o dedo não deixa colar. Agora tem um botão
  *Colar* que põe o que você copiou e já confere. No app também tem *Colar chave*.
- **A chave posta pelo app não fica mais pedindo outra:** se o painel dela estava pedindo a chave e você pôs pelo app,
  o painel some sozinho.
- **Barra de música do seu jeito:** em *Ajustes → Barra de música* escolha *Sempre* (fica enquanto toca), *Um tempo*
  (como antes) ou *Nunca*.
- **Avisos mais em cima:** os cartões das suas notificações não aparecem mais no meio da tela, e sim em cima, bem
  onde a Capri ainda consegue subir.
- **A Capri não se perde mais:** às vezes ela espiava pela borda de cima, que no celular fica coberta pela barra de
  status, e parecia que tinha sumido. Agora ela só espia pelos lados, e nunca fica escondida.
- **A notificação dela, mais limpa:** não repete mais cada notificação ou música que chega (o Android já mostra),
  diz “Na sua tela. Toque aqui para abrir meu menu.”, e a última coisa que ela disse some depois de 10 minutos.

## 1.5.0

- **A Capri não sai mais disparada:** ao tocar nela ou soltar depois de arrastar, às vezes ela saía voando como se
  você tivesse jogado. Agora ela só é lançada se você jogar de verdade com um movimento rápido; se só soltar, ela cai
  suave. E quando você joga, ela vai com menos força.
- **Escolha o tamanho dela:** em *Ajustes → Capri na sua tela*, de mini a enorme.
- **Modo fantasma:** a Capri continua na sua tela, mas não pode ser tocada e não atrapalha: seus toques passam para
  o app de baixo, como um shimeji fantasma. Liga em *Ajustes* ou no menu dela.
- **Mais movimento:** ela não fica mais parada. Escolha o quanto ela é agitada em *Ajustes → Movimento*: calma (como
  antes), normal ou agitada. Ela anda, pula, gira, se espreguiça e olha em volta sozinha.
- **A Capri vê sua tela:** diga “Capri, olha minha tela” (ou toque em *Olhar minha tela agora* em Ajustes, ou no menu
  dela) e ela comenta o que vê. Com *Ver ao vivo* ela continua olhando e reage sozinha de vez em quando, ou quando você
  troca de app, até você desligar. O Android pergunta antes toda vez que ela começa a ver, e você vê o ícone de
  transmissão enquanto ela olha. As capturas não são salvas: só vão para a IA dela. Apps protegidos (bancos, senhas)
  aparecem pretos para ela. Precisa de uma IA que enxergue imagens, como o Gemini (a chave é grátis); a IA grátis não
  enxerga, e a Capri te avisa.
- **O chat não trava mais:** agora sabemos por que demorava: a IA grátis só deixa uma resposta a cada 45 segundos por
  celular, e se você escrevia antes, ela ficava esperando ou falhava. Agora a Capri espera a vez dela e pergunta de
  novo sozinha, pede para a IA pensar menos para responder mais rápido, e enquanto isso te diz que está pensando em
  vez de ficar muda. Se quiser respostas na hora, coloque uma chave grátis do Gemini.
- **Ela escolhe um modelo melhor sozinha:** prefere os modelos rápidos e que são mesmo de chat, e lembra a lista por
  alguns minutos para não perguntar toda vez.
- **Sua chave de IA não é mais apagada:** às vezes, se a IA demorava ou falhava uma vez, a Capri descartava a chave e
  dizia que não tinha IA. Agora ela só tira se a chave for mesmo inválida.
- **Ela fala mais como a Capri:** no chat ela responde como alguém com quem você conversa, não como assistente: reage,
  brinca, opina e responde ao que você realmente disse. Em espanhol ela fala como latina, com um pouco de gíria
  mexicana.

## 1.4.1

- **Não tem mais zonas mortas do lado da Capri:** depois de se teletransportar, tocar perto dela (não nela) não
  respondia, nem a Capri nem o app de baixo, até você mover ela. A área de toque agora segue o contorno do corpo dela
  em vez de um quadrado grande em volta, e enquanto ela se teletransporta não bloqueia nada.
- **Atualizar pelo app em mais celulares:** em alguns (como Samsung com Android 16) a instalação falhava com “Self
  update is blocked”. Agora a versão nova abre na tela de instalação do Android, igual a se você abrisse pelo
  navegador. Se mesmo assim falhar, abre a página dela para baixar.

## 1.4.0

- **Cartões com animação:** os avisos das suas notificações entram deslizando com um glitch e saem do mesmo jeito,
  não aparecem e somem de repente. Uma barrinha vermelha embaixo mostra quanto tempo falta na tela.
- **A barra de música não fica mais sempre:** aparece quando uma música começa, muda, ou você pausa ou retoma, e some
  sozinha depois de 15 segundos. Se você tocar nela, fica mais um pouco. Em pausa, some depois de 12 segundos.
- **Nova aba “Comandos”:** tudo o que você pode pedir para a Capri, em voz alta ou escrito no chat dela, com exemplos.
- **Atualize pelo app de novo:** *Atualizar* baixa e instala a versão nova ali mesmo, sem passar pelo navegador (o
  Android pede para confirmar). Na primeira vez, pede para deixar a Capri instalar apps.
- **Aviso do sistema quando tem versão nova:** o app confere a cada 3 horas, mesmo com a Capri desligada, e te manda
  uma notificação. Ao tocar nela, o app abre e a atualização começa.
- **Novidades fechadas:** cada versão aparece fechada, com quantas mudanças traz; toque para ler.
- **“Fale com ela” mais fluido:** quando você diz só “Capri”, ela responde no painel sem falar, para não cobrir o que
  você diz depois, e espera um pouco mais você terminar a frase.
