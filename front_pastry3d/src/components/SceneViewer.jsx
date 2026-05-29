import { Suspense, useMemo } from "react";
import { Canvas } from "@react-three/fiber";
import { Environment, OrbitControls, useGLTF } from "@react-three/drei";
import * as THREE from "three";

function parseJson(value, fallback) {
  if (!value) return fallback;
  if (typeof value === "object") return value;

  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

function getAssetUrl(modelUrl) {
  if (!modelUrl) return "";
  if (modelUrl.startsWith("http://") || modelUrl.startsWith("https://")) {
    return modelUrl;
  }

  return modelUrl;
}

function vectorFrom(value, fallback = [0, 0, 0]) {
  if (!value) return fallback;

  if (Array.isArray(value)) {
    return [
      Number(value[0] || 0),
      Number(value[1] || 0),
      Number(value[2] || 0),
    ];
  }

  if (typeof value === "object") {
    return [
      Number(value.x || 0),
      Number(value.y || 0),
      Number(value.z || 0),
    ];
  }

  return fallback;
}

function normalizeScaleValue(value) {
  if (Array.isArray(value)) {
    return [
      Number(value[0] || 1),
      Number(value[1] || 1),
      Number(value[2] || 1),
    ];
  }

  if (typeof value === "object" && value !== null) {
    return [
      Number(value.x || 1),
      Number(value.y || 1),
      Number(value.z || 1),
    ];
  }

  const numberValue = Number(value || 1);
  return [numberValue, numberValue, numberValue];
}

function multiplyScaleValue(value, multiplier) {
  const base = normalizeScaleValue(value);
  return base.map((item) => item * multiplier);
}

function applyColorOverrides(root, colorOverrides) {
  if (!root || !colorOverrides || typeof colorOverrides !== "object") return;

  root.traverse((child) => {
    if (!child.isMesh || !child.material) return;

    const materialName = child.material.name || "";
    const meshName = child.name || "";
    const override = colorOverrides[materialName] || colorOverrides[meshName];

    if (override) {
      child.material = child.material.clone();
      child.material.color.set(override);
    }
  });
}

function ModelItem({ modelUrl, position = [0, 0, 0], rotation = [0, 0, 0], scale = 1, colorOverrides }) {
  const url = getAssetUrl(modelUrl);
  const gltf = useGLTF(url);

  const normalizedModel = useMemo(() => {
    const root = gltf.scene.clone(true);

    root.traverse((child) => {
      if (child.isMesh) {
        child.castShadow = true;
        child.receiveShadow = true;

        if (child.material) {
          child.material = child.material.clone();
        }
      }
    });

    applyColorOverrides(root, colorOverrides);

    const box = new THREE.Box3().setFromObject(root);
    const size = new THREE.Vector3();
    const center = new THREE.Vector3();

    box.getSize(size);
    box.getCenter(center);

    const maxAxis = Math.max(size.x, size.y, size.z) || 1;
    const isGenerated = String(modelUrl || "").includes("/generated/");

    return {
      root,
      center: isGenerated ? center.toArray() : [0, 0, 0],
      autoScale: isGenerated ? 2.8 / maxAxis : 1,
    };
  }, [gltf.scene, modelUrl, colorOverrides]);

  return (
    <group
      position={vectorFrom(position)}
      rotation={vectorFrom(rotation)}
      scale={multiplyScaleValue(scale, normalizedModel.autoScale)}
    >
      <group position={normalizedModel.center.map((value) => -value)}>
        <primitive object={normalizedModel.root} />
      </group>
    </group>
  );
}

function SceneObjects({ scene }) {
  if (!scene) return null;

  if (scene.modelUrl) {
    return (
      <ModelItem
        modelUrl={scene.modelUrl}
        position={scene.position || [0, 0, 0]}
        rotation={scene.rotation || [0, 0, 0]}
        scale={scene.scale || 1}
        colorOverrides={scene.colorOverrides}
      />
    );
  }

  const base = scene.base || null;
  const parts = Array.isArray(scene.parts) ? scene.parts : [];

  return (
    <>
      {base?.modelUrl && (
        <ModelItem
          modelUrl={base.modelUrl}
          position={base.position || [0, 0, 0]}
          rotation={base.rotation || [0, 0, 0]}
          scale={base.scale || 1}
          colorOverrides={base.colorOverrides}
        />
      )}

      {parts.map((part, index) => (
        <ModelItem
          key={`${part.modelUrl || "part"}-${index}`}
          modelUrl={part.modelUrl}
          position={part.position || [0, 0, 0]}
          rotation={part.rotation || [0, 0, 0]}
          scale={part.scale || 1}
          colorOverrides={part.colorOverrides}
        />
      ))}
    </>
  );
}

function SceneFallback() {
  return (
    <div className="viewer-placeholder">
      <strong>Cargando modelo 3D...</strong>
    </div>
  );
}

export default function SceneViewer({ sceneJson, modelUrl, status }) {
  const scene = parseJson(sceneJson, {});
  const normalizedScene = modelUrl && !scene?.base && !scene?.modelUrl
    ? { modelUrl }
    : scene;

  const hasScene = Boolean(
    normalizedScene?.modelUrl ||
    normalizedScene?.base?.modelUrl ||
    (Array.isArray(normalizedScene?.parts) && normalizedScene.parts.length > 0)
  );

  if (!hasScene) {
    return (
      <div className="scene-viewer pending">
        <div className="viewer-placeholder">
          <strong>{status === "PENDING_MANUAL_MODEL" ? "Modelo pendiente" : "Sin escena 3D"}</strong>
          <p>
            La receta se genero, pero todavia falta agregar uno o mas modelos GLB a la biblioteca para construir la escena.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="scene-viewer">
      <Suspense fallback={<SceneFallback />}>
        <Canvas camera={{ position: [2.8, 2.1, 3.2], fov: 35 }}>
          <ambientLight intensity={0.9} />
          <directionalLight position={[4, 6, 5]} intensity={1.4} castShadow />
          <directionalLight position={[-3, 2, -4]} intensity={0.45} />

          <group position={[0, -0.15, 0]}>
            <SceneObjects scene={normalizedScene} />
          </group>

          <Environment preset="studio" />
          <OrbitControls enablePan={false} minDistance={0.8} maxDistance={6} />
        </Canvas>
      </Suspense>
    </div>
  );
}
