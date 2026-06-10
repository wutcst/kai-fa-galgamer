function DualSpriteStage({ dialogue, sceneUrl, assets, children, onStageClick }) {
  const left = dialogue.leftCharacter ?? {}
  const right = dialogue.rightCharacter ?? {}
  const dimmed = dialogue.nodeType === 'CHOICE'

  return (
    <section
      className={`dual-sprite-stage${dimmed ? ' is-choice' : ''}`}
      style={{ backgroundImage: `url("${sceneUrl}")` }}
      onMouseDown={onStageClick}
    >
      <div className="dual-stage-vignette" />
      <CharacterSprite side="left" character={left} assets={assets} dimmed={dimmed} />
      <CharacterSprite side="right" character={right} assets={assets} dimmed={dimmed} />
      <div className="adv-stage-overlay">{children}</div>
    </section>
  )
}

function CharacterSprite({ side, character, assets, dimmed }) {
  const fallback = fallbackSprite(assets)
  const src = spriteUrl(character, assets, fallback)
  const speaking = Boolean(character.isSpeaking) && !dimmed

  return (
    <img
      className={`adv-character adv-character-${side}${speaking ? ' is-speaking' : ' is-muted'}`}
      src={src}
      alt=""
      onError={(event) => {
        if (event.currentTarget.dataset.fallbackApplied !== 'true') {
          event.currentTarget.dataset.fallbackApplied = 'true'
          event.currentTarget.src = fallback
        }
      }}
    />
  )
}

function spriteUrl(character, assets, fallback) {
  return assets[character.assetKey]
    ?? assets[`character.${character.id}.default`]
    ?? assets['character.silhouette_fallback']
    ?? fallback
}

function fallbackSprite(assets) {
  return assets['character.silhouette_fallback']
    ?? assets['character.zuul_overlord']
    ?? assets['ui.player_avatar']
    ?? assets.fallback
}

export default DualSpriteStage
